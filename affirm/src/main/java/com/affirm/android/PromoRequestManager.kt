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
 * Tracks promo requests for a host [Lifecycle] and cancels them on `ON_DESTROY`.
 *
 * The host is resolved from the view: the view lifecycle of an AndroidX Fragment first, then a
 * [LifecycleOwner] Activity. Otherwise the request is not bound to any lifecycle.
 */
@MainThread
internal class PromoRequestManager private constructor() : LifecycleEventObserver {

    // In-flight requests are held strongly by their OkHttp callback and finished ones by the
    // view tag, so a weak set lets discarded views and their requests be collected.
    private val requests: MutableSet<PromoRequest> = Collections.newSetFromMap(WeakHashMap())

    private fun add(request: PromoRequest) {
        requests.add(request)
    }

    private fun remove(request: PromoRequest) {
        requests.remove(request)
    }

    private fun size(): Int = requests.size

    private fun cancelAll() {
        // Not toList(): for a single element it calls first(), which throws if GC clears the
        // entry after size() is read. toArray() tolerates entries cleared mid-iteration.
        requests.toTypedArray().forEach { it.cancel() }
        requests.clear()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event != Lifecycle.Event.ON_DESTROY) {
            return
        }
        cancelAll()
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
            val previous = view.getTag(R.id.affirm_promo_request)
            if (previous is PromoRequest) {
                untrack(previous)
            }
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
                managerFor(owner.lifecycle).add(request)
            }
            request.create()
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

        private fun untrack(request: PromoRequest) {
            request.cancel()
            lifecycleToManager.values.forEach { it.remove(request) }
        }

        @VisibleForTesting
        fun trackedRequestCount(lifecycle: Lifecycle): Int {
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
                manager.cancelAll()
            }
            lifecycleToManager.clear()
        }
    }
}
