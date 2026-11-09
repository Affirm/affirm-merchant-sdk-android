package com.affirm.android

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.util.Collections
import java.util.WeakHashMap

/**
 * Tracks the views showing promo messages for a host. When the host is destroyed it cancels
 * their requests and destroys [AffirmPromotionButton]s, so merchants do not have to call
 * [AffirmPromotionButton.destroy] for every button, e.g. in a list.
 *
 * The host is resolved from the view once it is attached: the view lifecycle of an AndroidX
 * Fragment first, then a [LifecycleOwner] Activity, then any other Activity through
 * [Application.ActivityLifecycleCallbacks].
 */
@MainThread
internal class PromoRequestManager private constructor() : LifecycleEventObserver {

    // A view's current request is kept in its tag. Views are held by their window, or by the
    // callback of an in-flight request, so a weak set lets discarded views be collected.
    private val views: MutableSet<View> = Collections.newSetFromMap(WeakHashMap())

    private fun add(view: View) {
        views.add(view)
    }

    private fun remove(view: View) {
        views.remove(view)
    }

    private fun size(): Int = views.size

    private fun releaseAll() {
        // Not toList(): for a single element it calls first(), which throws if GC clears the
        // entry after size() is read. toArray() tolerates entries cleared mid-iteration.
        views.toTypedArray().forEach { release(it) }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event != Lifecycle.Event.ON_DESTROY) {
            return
        }
        releaseAll()
        source.lifecycle.removeObserver(this)
        lifecycleToManager.remove(source.lifecycle)
    }

    companion object {
        private val lifecycleToManager = HashMap<Lifecycle, PromoRequestManager>()
        private val activityToManager = HashMap<Activity, PromoRequestManager>()
        private var callbacksApplication: Application? = null

        private val activityCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityDestroyed(activity: Activity) {
                activityToManager.remove(activity)?.releaseAll()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        }

        /**
         * Cancels the request previously started for [view], then starts [request] and
         * cancels it when the resolved host is destroyed.
         *
         * @return false if the host is already finishing or destroyed, so [request] never
         * starts and its callback is never called.
         */
        @JvmStatic
        @MainThread
        fun track(view: View, request: PromoRequest): Boolean {
            untrack(view)
            view.setTag(R.id.affirm_promo_request, request)

            val activity = AffirmUtils.getActivityFromView(view)
            if (activity != null && (activity.isFinishing || activity.isDestroyed)) {
                return false
            }
            if (view.isAttachedToWindow) {
                if (!bind(view)) {
                    return false
                }
            } else {
                bindOnAttach(view, request)
            }
            request.create()
            return true
        }

        /** Ends what [view] shows as if its host were destroyed. */
        private fun release(view: View) {
            if (view is AffirmPromotionButton) view.destroy() else untrack(view)
        }

        /** Returns false if the resolved host is already destroyed. */
        private fun bind(view: View): Boolean {
            val activity = AffirmUtils.getActivityFromView(view)
            val owner = findLifecycleOwner(view, activity)
            // A destroyed host never reports its destruction again, so its manager would never
            // leave the map.
            when {
                owner != null -> {
                    if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return false
                    managerFor(owner.lifecycle).add(view)
                }
                activity != null -> {
                    if (activity.isDestroyed) return false
                    managerFor(activity).add(view)
                }
                else -> AffirmLog.w("Promo view has no Activity. Request will not be cancelled.")
            }
            return true
        }

        /**
         * Before attach, e.g. in `Activity.onCreate` or `Fragment.onCreateView`, the Fragment
         * that will hold the view cannot be found yet.
         */
        private fun bindOnAttach(view: View, request: PromoRequest) {
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.removeOnAttachStateChangeListener(this)
                    if (v.getTag(R.id.affirm_promo_request) === request && !bind(v)) {
                        release(v)
                    }
                }

                override fun onViewDetachedFromWindow(v: View) {}
            })
        }

        /** Cancels the request started for [view], if any, and forgets [view]. */
        @JvmStatic
        @MainThread
        fun untrack(view: View) {
            cancelRequest(view)
            lifecycleToManager.values.forEach { it.remove(view) }
            activityToManager.values.forEach { it.remove(view) }
        }

        /**
         * Cancels the request started for [view], if any, but keeps [view], so the host's
         * `ON_DESTROY` still destroys it, e.g. a detached list row that keeps its WebView.
         */
        @JvmStatic
        @MainThread
        fun cancelRequest(view: View) {
            (view.getTag(R.id.affirm_promo_request) as? PromoRequest)?.cancel()
            view.setTag(R.id.affirm_promo_request, null)
        }

        private fun managerFor(lifecycle: Lifecycle): PromoRequestManager {
            return lifecycleToManager.getOrPut(lifecycle) {
                PromoRequestManager().also { lifecycle.addObserver(it) }
            }
        }

        /** For an Activity that is not a [LifecycleOwner]. */
        private fun managerFor(activity: Activity): PromoRequestManager {
            val application = activity.application
            if (callbacksApplication !== application) {
                callbacksApplication?.unregisterActivityLifecycleCallbacks(activityCallbacks)
                application.registerActivityLifecycleCallbacks(activityCallbacks)
                callbacksApplication = application
            }
            return activityToManager.getOrPut(activity) { PromoRequestManager() }
        }

        private fun findLifecycleOwner(view: View, activity: Activity?): LifecycleOwner? {
            if (activity is FragmentActivity) {
                // The view lifecycle ends on onDestroyView, e.g. when the Fragment goes to the
                // back stack, while the Fragment itself stays alive.
                findSupportFragment(view, activity)?.let { return it.viewLifecycleOwner }
            }
            return activity as? LifecycleOwner
        }

        private fun findSupportFragment(
            target: View,
            activity: FragmentActivity
        ): Fragment? {
            val viewToFragment = HashMap<View, Fragment>()
            collectFragmentsWithViews(
                activity.supportFragmentManager.fragments,
                viewToFragment
            )

            var current: View? = target
            val activityRoot = activity.findViewById<View>(android.R.id.content)
            while (current != null) {
                val result = viewToFragment[current]
                if (result != null) {
                    return result
                }
                if (current == activityRoot) {
                    break
                }
                current = current.parent as? View
            }
            return null
        }

        private fun collectFragmentsWithViews(
            fragments: Collection<Fragment?>?,
            result: MutableMap<View, Fragment>
        ) {
            if (fragments == null) {
                return
            }
            for (fragment in fragments) {
                // Older FragmentManagers may return null entries.
                val fragmentView = fragment?.view ?: continue
                result[fragmentView] = fragment
                collectFragmentsWithViews(fragment.childFragmentManager.fragments, result)
            }
        }

        @VisibleForTesting
        fun trackedViewCount(lifecycle: Lifecycle): Int {
            return lifecycleToManager[lifecycle]?.size() ?: 0
        }

        @VisibleForTesting
        fun hasManager(lifecycle: Lifecycle): Boolean {
            return lifecycleToManager.containsKey(lifecycle)
        }

        @VisibleForTesting
        fun hasManager(activity: Activity): Boolean {
            return activityToManager.containsKey(activity)
        }

        @VisibleForTesting
        fun reset() {
            lifecycleToManager.entries.toList().forEach { (lifecycle, manager) ->
                lifecycle.removeObserver(manager)
                manager.releaseAll()
            }
            lifecycleToManager.clear()
            activityToManager.values.toList().forEach { it.releaseAll() }
            activityToManager.clear()
            callbacksApplication?.unregisterActivityLifecycleCallbacks(activityCallbacks)
            callbacksApplication = null
        }
    }
}
