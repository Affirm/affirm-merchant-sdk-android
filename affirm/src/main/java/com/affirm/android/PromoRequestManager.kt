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
 * Tracks promo requests for a host and cancels them when the host is destroyed.
 *
 * The host is resolved from the view once it is attached: the view lifecycle of an AndroidX
 * Fragment first, then a [LifecycleOwner] Activity, then any other Activity through
 * [Application.ActivityLifecycleCallbacks].
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
        private val activityToManager = HashMap<Activity, PromoRequestManager>()
        private var callbacksApplication: Application? = null

        private val activityCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityDestroyed(activity: Activity) {
                activityToManager.remove(activity)?.cancelAll()
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
            if (view.isAttachedToWindow) {
                if (bind(view, request)) {
                    request.create()
                }
            } else {
                request.create()
                bindOnAttach(view, request)
            }
        }

        /** Returns false if the resolved host is already destroyed. */
        private fun bind(view: View, request: PromoRequest): Boolean {
            val activity = AffirmUtils.getActivityFromView(view)
            val owner = findLifecycleOwner(view, activity)
            // A destroyed host never reports its destruction again, so its manager would never
            // leave the map.
            when {
                owner != null -> {
                    if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return false
                    managerFor(owner.lifecycle).add(request)
                }
                activity != null -> {
                    if (activity.isDestroyed) return false
                    managerFor(activity).add(request)
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
                    if (v.getTag(R.id.affirm_promo_request) === request && !bind(v, request)) {
                        request.cancel()
                    }
                }

                override fun onViewDetachedFromWindow(v: View) {}
            })
        }

        /** Cancels and forgets the request started for [view], if any. */
        @JvmStatic
        @MainThread
        fun untrack(view: View) {
            val request = view.getTag(R.id.affirm_promo_request)
            if (request is PromoRequest) {
                untrack(request)
            }
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

        private fun untrack(request: PromoRequest) {
            request.cancel()
            lifecycleToManager.values.forEach { it.remove(request) }
            activityToManager.values.forEach { it.remove(request) }
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
        fun hasManager(activity: Activity): Boolean {
            return activityToManager.containsKey(activity)
        }

        @VisibleForTesting
        fun reset() {
            lifecycleToManager.entries.toList().forEach { (lifecycle, manager) ->
                lifecycle.removeObserver(manager)
                manager.cancelAll()
            }
            lifecycleToManager.clear()
            activityToManager.values.forEach { it.cancelAll() }
            activityToManager.clear()
            callbacksApplication?.unregisterActivityLifecycleCallbacks(activityCallbacks)
            callbacksApplication = null
        }
    }
}
