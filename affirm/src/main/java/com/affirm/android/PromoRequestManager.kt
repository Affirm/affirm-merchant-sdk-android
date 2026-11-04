package com.affirm.android

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
 * Tracks the views showing promo messages for a host [Lifecycle]. On `ON_DESTROY` it cancels
 * their requests and destroys [AffirmPromotionButton]s, so merchants do not have to call
 * [AffirmPromotionButton.destroy] for every button, e.g. in a list.
 *
 * The host is resolved from the view: the view lifecycle of an AndroidX Fragment first, then a
 * [LifecycleOwner] Activity. Otherwise the view is not bound to any lifecycle.
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
        views.toTypedArray().forEach { view ->
            if (view is AffirmPromotionButton) view.destroy() else untrack(view)
        }
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

        /**
         * Cancels the request previously started for [view], then starts [request] and
         * cancels it when the resolved host is destroyed.
         */
        @JvmStatic
        @MainThread
        fun track(view: View, request: PromoRequest) {
            untrack(view)
            view.setTag(R.id.affirm_promo_request, request)

            val activity = AffirmUtils.getActivityFromView(view)
            if (activity != null && (activity.isFinishing || activity.isDestroyed)) {
                return
            }
            val owner = findLifecycleOwner(view)
            if (owner == null) {
                AffirmLog.w("Host is not a LifecycleOwner. Promo request will not be cancelled.")
            } else {
                // A destroyed Lifecycle never dispatches ON_DESTROY to a new observer, so its
                // manager would never leave the map.
                if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
                    return
                }
                managerFor(owner.lifecycle).add(view)
            }
            request.create()
        }

        /** Cancels the request started for [view], if any, and forgets [view]. */
        @JvmStatic
        @MainThread
        fun untrack(view: View) {
            (view.getTag(R.id.affirm_promo_request) as? PromoRequest)?.cancel()
            view.setTag(R.id.affirm_promo_request, null)
            lifecycleToManager.values.forEach { it.remove(view) }
        }

        private fun managerFor(lifecycle: Lifecycle): PromoRequestManager {
            return lifecycleToManager.getOrPut(lifecycle) {
                PromoRequestManager().also { lifecycle.addObserver(it) }
            }
        }

        private fun findLifecycleOwner(view: View): LifecycleOwner? {
            val activity = AffirmUtils.getActivityFromView(view)
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
        fun reset() {
            lifecycleToManager.entries.toList().forEach { (lifecycle, manager) ->
                lifecycle.removeObserver(manager)
                manager.releaseAll()
            }
            lifecycleToManager.clear()
        }
    }
}
