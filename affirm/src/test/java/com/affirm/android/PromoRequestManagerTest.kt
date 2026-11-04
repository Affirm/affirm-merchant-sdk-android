package com.affirm.android

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PromoRequestManagerTest {

    private lateinit var activity: TestLifecycleActivity

    @Before
    fun setup() {
        activity = Robolectric.buildActivity(TestLifecycleActivity::class.java).get()
        activity.registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    private fun attachedView(): View = AttachedView(activity)

    @After
    fun tearDown() {
        PromoRequestManager.reset()
    }

    @Test
    fun registersOneObserverForManyRequests() {
        val views = (0 until 100).map { attachedView() }
        views.forEach { PromoRequestManager.track(it, mock(PromoRequest::class.java)) }

        assertThat(activity.registry.observerCount).isEqualTo(1)
        assertThat(PromoRequestManager.trackedViewCount(activity.registry)).isEqualTo(100)
        // Also keeps the views reachable until here: the manager holds them weakly, so the
        // count above could otherwise drop under GC.
        views.forEach { assertThat(it.getTag(R.id.affirm_promo_request)).isNotNull() }
    }

    @Test
    fun reconfiguringViewCancelsPreviousRequests() {
        val view = attachedView()
        val requests = (0 until 100).map { mock(PromoRequest::class.java) }

        requests.forEach { PromoRequestManager.track(view, it) }

        requests.dropLast(1).forEach { verify(it).cancel() }
        val last = requests.last()
        verify(last).create()
        verify(last, never()).cancel()
        assertThat(view.getTag(R.id.affirm_promo_request)).isSameAs(last)
        assertThat(PromoRequestManager.trackedViewCount(activity.registry)).isEqualTo(1)
        assertThat(activity.registry.observerCount).isEqualTo(1)
    }

    @Test
    fun untrackCancelsAndForgetsViewRequest() {
        val view = attachedView()
        val request = mock(PromoRequest::class.java)
        PromoRequestManager.track(view, request)
        assertThat(PromoRequestManager.trackedViewCount(activity.registry)).isEqualTo(1)

        PromoRequestManager.untrack(view)

        verify(request).cancel()
        assertThat(view.getTag(R.id.affirm_promo_request)).isNull()
        assertThat(PromoRequestManager.trackedViewCount(activity.registry)).isEqualTo(0)
    }

    @Test
    fun cancelsRequestsAndUnregistersOnDestroy() {
        val request = mock(PromoRequest::class.java)
        PromoRequestManager.track(attachedView(), request)

        verify(request, never()).cancel()
        activity.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(request).cancel()
        assertThat(activity.registry.observerCount).isEqualTo(0)
        assertThat(PromoRequestManager.hasManager(activity.registry)).isFalse()
    }

    @Test
    fun destroysPromotionButtonsOnDestroy() {
        val button = spy(AffirmPromotionButton(activity))
        doReturn(true).`when`(button).isAttachedToWindow
        val request = mock(PromoRequest::class.java)
        PromoRequestManager.track(button, request)

        activity.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(button).destroy()
        verify(request).cancel()
    }

    @Test
    fun releasesDiscardedViewAndRequestBeforeDestroy() {
        val ref = trackUnreferencedRequest()

        // WeakHashMap only drops an entry after the cleared reference is enqueued, which
        // happens on another thread some time after ref.get() returns null.
        var i = 0
        while (i < 100 && PromoRequestManager.trackedViewCount(activity.registry) != 0) {
            System.gc()
            System.runFinalization()
            Thread.sleep(10)
            i++
        }

        assertThat(ref.get()).isNull()
        assertThat(PromoRequestManager.trackedViewCount(activity.registry)).isEqualTo(0)
        assertThat(activity.registry.observerCount).isEqualTo(1)
    }

    @Test
    fun doesNotStartIfActivityAlreadyDestroyed() {
        val destroyed = mock(TestLifecycleActivity::class.java)
        `when`(destroyed.isDestroyed).thenReturn(true)
        val view = mock(View::class.java)
        `when`(view.context).thenReturn(destroyed)
        val request = mock(PromoRequest::class.java)

        PromoRequestManager.track(view, request)

        verify(request, never()).create()
    }

    @Test
    fun doesNotStartIfActivityFinishing() {
        val finishing = mock(TestLifecycleActivity::class.java)
        `when`(finishing.isFinishing).thenReturn(true)
        val view = mock(View::class.java)
        `when`(view.context).thenReturn(finishing)
        val request = mock(PromoRequest::class.java)

        PromoRequestManager.track(view, request)

        verify(request, never()).create()
    }

    @Test
    fun doesNotStartOrRegisterIfLifecycleAlreadyDestroyed() {
        activity.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        val request = mock(PromoRequest::class.java)

        PromoRequestManager.track(attachedView(), request)

        verify(request, never()).create()
        assertThat(PromoRequestManager.hasManager(activity.registry)).isFalse()
        assertThat(activity.registry.observerCount).isEqualTo(0)
    }

    @Test
    fun startsButDoesNotBindViewOutsideActivity() {
        val request = mock(PromoRequest::class.java)

        PromoRequestManager.track(AttachedView(activity.applicationContext), request)

        verify(request).create()
        assertThat(PromoRequestManager.hasManager(activity)).isFalse()
        assertThat(PromoRequestManager.hasManager(activity.registry)).isFalse()
    }

    @Test
    fun cancelsRawActivityRequestsOnDestroy() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val host = controller.get()
        val view = View(host)
        host.setContentView(view)
        val request = mock(PromoRequest::class.java)

        PromoRequestManager.track(view, request)

        verify(request).create()
        assertThat(PromoRequestManager.hasManager(host)).isTrue()

        controller.pause().stop().destroy()

        verify(request).cancel()
        assertThat(PromoRequestManager.hasManager(host)).isFalse()
    }

    @Test
    fun rawActivityRequestStartedInOnCreateIsCancelledOnDestroy() {
        val controller = Robolectric.buildActivity(Activity::class.java).create()
        val host = controller.get()
        val view = View(host)
        host.setContentView(view)
        val request = mock(PromoRequest::class.java)

        PromoRequestManager.track(view, request)

        assertThat(PromoRequestManager.hasManager(host)).isFalse()

        controller.start().resume().visible()

        assertThat(PromoRequestManager.hasManager(host)).isTrue()
        verify(request, never()).cancel()

        controller.pause().stop().destroy()

        verify(request).cancel()
    }

    @Test
    fun reconfiguringViewInRawActivityCancelsPreviousRequest() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val view = View(controller.get())
        controller.get().setContentView(view)
        val first = mock(PromoRequest::class.java)
        val second = mock(PromoRequest::class.java)

        PromoRequestManager.track(view, first)
        PromoRequestManager.track(view, second)

        verify(first).cancel()
        verify(second).create()
        verify(second, never()).cancel()
        assertThat(view.getTag(R.id.affirm_promo_request)).isSameAs(second)

        controller.pause().stop().destroy()

        // Only the current request is cancelled on destroy; the replaced one is not tracked.
        verify(first, times(1)).cancel()
        verify(second).cancel()
    }

    @Test
    fun bindsToFragmentViewLifecycleInsteadOfActivity() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = PromoFragment()
        host.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()

        val request = mock(PromoRequest::class.java)
        PromoRequestManager.track(fragment.promoView, request)
        val viewLifecycle = fragment.viewLifecycleOwner.lifecycle

        assertThat(PromoRequestManager.hasManager(host.lifecycle)).isFalse()
        assertThat(PromoRequestManager.hasManager(fragment.lifecycle)).isFalse()
        assertThat(PromoRequestManager.hasManager(viewLifecycle)).isTrue()
        verify(request, never()).cancel()

        host.supportFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commitNow()

        verify(request).cancel()
        assertThat(PromoRequestManager.hasManager(viewLifecycle)).isFalse()
    }

    @Test
    fun cancelsWhenFragmentViewIsDestroyedButFragmentStays() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = PromoFragment()
        host.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val request = mock(PromoRequest::class.java)
        PromoRequestManager.track(fragment.promoView, request)
        val viewLifecycle = fragment.viewLifecycleOwner.lifecycle

        // Like going to the back stack: onDestroyView runs, the Fragment is not destroyed.
        host.supportFragmentManager
            .beginTransaction()
            .detach(fragment)
            .commitNow()

        assertThat(fragment.lifecycle.currentState).isNotEqualTo(Lifecycle.State.DESTROYED)
        verify(request).cancel()
        assertThat(PromoRequestManager.hasManager(viewLifecycle)).isFalse()
    }

    @Test
    fun destroyingOneFragmentOnlyDestroysItsOwnButtons() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val first = PromoFragment()
        val second = PromoFragment()
        host.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, first)
            .add(android.R.id.content, second)
            .commitNow()
        val firstButton = first.addButton()
        val secondButton = second.addButton()
        val firstRequest = mock(PromoRequest::class.java)
        val secondRequest = mock(PromoRequest::class.java)
        PromoRequestManager.track(firstButton, firstRequest)
        PromoRequestManager.track(secondButton, secondRequest)

        host.supportFragmentManager
            .beginTransaction()
            .remove(first)
            .commitNow()

        verify(firstRequest).cancel()
        assertThat(firstButton.getTag(R.id.affirm_promo_request)).isNull()
        verify(secondRequest, never()).cancel()
        assertThat(secondButton.getTag(R.id.affirm_promo_request)).isSameAs(secondRequest)
        assertThat(PromoRequestManager.trackedViewCount(second.viewLifecycleOwner.lifecycle)).isEqualTo(1)
    }

    @Test
    fun bindsToActivityWhenViewIsOutsideFragments() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = PromoFragment()
        host.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val outside = View(host)
        host.findViewById<ViewGroup>(android.R.id.content).addView(outside)

        PromoRequestManager.track(outside, mock(PromoRequest::class.java))

        assertThat(PromoRequestManager.hasManager(host.lifecycle)).isTrue()
        assertThat(PromoRequestManager.hasManager(fragment.viewLifecycleOwner.lifecycle))
            .isFalse()
    }

    @Test
    fun bindsToInnermostChildFragment() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val parent = PromoFragment()
        host.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, parent)
            .commitNow()
        val child = PromoFragment()
        parent.childFragmentManager
            .beginTransaction()
            .add(parent.containerId, child)
            .commitNow()

        PromoRequestManager.track(child.promoView, mock(PromoRequest::class.java))

        assertThat(PromoRequestManager.hasManager(child.viewLifecycleOwner.lifecycle)).isTrue()
        assertThat(PromoRequestManager.hasManager(parent.viewLifecycleOwner.lifecycle)).isFalse()
        assertThat(PromoRequestManager.hasManager(host.lifecycle)).isFalse()
    }

    @Test
    fun requestStartedInActivityOnCreateBindsOnceAttached() {
        val controller = Robolectric.buildActivity(FragmentActivity::class.java).create()
        val host = controller.get()
        val view = View(host)
        host.setContentView(view)
        val request = mock(PromoRequest::class.java)

        PromoRequestManager.track(view, request)

        assertThat(view.isAttachedToWindow).isFalse()
        verify(request).create()
        assertThat(PromoRequestManager.hasManager(host.lifecycle)).isFalse()

        controller.start().resume().visible()

        assertThat(PromoRequestManager.trackedViewCount(host.lifecycle)).isEqualTo(1)
        verify(request, never()).cancel()

        controller.pause().stop().destroy()

        verify(request).cancel()
    }

    @Test
    fun requestStartedInOnCreateViewIsCancelledWithFragmentView() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val request = mock(PromoRequest::class.java)
        val fragment = TrackInOnCreateViewFragment(request)
        host.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val viewLifecycle = fragment.viewLifecycleOwner.lifecycle

        verify(request).create()
        assertThat(PromoRequestManager.trackedViewCount(viewLifecycle)).isEqualTo(1)
        assertThat(PromoRequestManager.trackedViewCount(host.lifecycle)).isEqualTo(0)

        host.supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, PromoFragment())
            .addToBackStack(null)
            .commit()
        host.supportFragmentManager.executePendingTransactions()

        assertThat(host.lifecycle.currentState).isNotEqualTo(Lifecycle.State.DESTROYED)
        verify(request).cancel()
    }

    @Test
    fun requestReplacedBeforeAttachIsNotRebound() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val first = mock(PromoRequest::class.java)
        val second = mock(PromoRequest::class.java)
        val fragment = TrackInOnCreateViewFragment(first, second)
        host.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        val viewLifecycle = fragment.viewLifecycleOwner.lifecycle

        verify(first).cancel()
        assertThat(PromoRequestManager.trackedViewCount(viewLifecycle)).isEqualTo(1)
        assertThat(PromoRequestManager.trackedViewCount(host.lifecycle)).isEqualTo(0)

        host.supportFragmentManager
            .beginTransaction()
            .remove(fragment)
            .commitNow()

        verify(second).cancel()
    }

    private fun trackUnreferencedRequest(): WeakReference<PromoRequest> {
        val request = NoOpPromoRequest()
        PromoRequestManager.track(attachedView(), request)
        assertThat(PromoRequestManager.trackedViewCount(activity.registry)).isEqualTo(1)
        return WeakReference(request)
    }

    private class NoOpPromoRequest : PromoRequest(
        null,
        null,
        BigDecimal.ONE,
        false,
        AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
        AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
        false,
        null,
        mock(SpannablePromoCallback::class.java)
    ) {
        override fun create() {}
        override fun cancel() {}
    }

    class PromoFragment : Fragment() {
        val containerId = View.generateViewId()
        lateinit var promoView: View

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            promoView = View(requireContext())
            return FrameLayout(requireContext()).apply {
                id = containerId
                addView(promoView)
            }
        }

        fun addButton(): AffirmPromotionButton {
            return AffirmPromotionButton(requireContext()).also {
                (requireView() as ViewGroup).addView(it)
            }
        }
    }

    /** Tracks [requests] in order from `onCreateView`, before the view is attached. */
    internal class TrackInOnCreateViewFragment(private vararg val requests: PromoRequest) : Fragment() {

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val promoView = View(requireContext())
            requests.forEach { PromoRequestManager.track(promoView, it) }
            return FrameLayout(requireContext()).apply { addView(promoView) }
        }
    }

    /** Stands in for a view in a window, which the unstarted test Activity does not have. */
    private class AttachedView(context: Context) : View(context) {
        override fun isAttachedToWindow(): Boolean = true
    }

    open class TestLifecycleActivity : Activity(), LifecycleOwner {
        val registry = LifecycleRegistry(this)

        override fun getLifecycle(): Lifecycle = registry
    }
}
