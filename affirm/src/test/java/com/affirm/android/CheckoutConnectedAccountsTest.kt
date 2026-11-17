package com.affirm.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.affirm.android.AffirmConstants.CHECKOUT_EXTRA
import com.affirm.android.AffirmConstants.CHECKOUT_SESSION_CODE
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_SESSION_CODE_PARAM
import com.affirm.android.model.AffirmPrepareAuthResult
import com.affirm.android.model.CheckoutResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CheckoutConnectedAccountsTest {

    @Before
    fun setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(
                Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            )
        }
        AffirmPlugins.get().consumeCodeVerifier()
    }

    @Test
    fun checkoutLoadsUrlWithSessionCodeAndVerifier() {
        val challenge = prepareAuth()
        val fragment = launchCheckout(sessionCode = "sc_1")

        fragment.innerCheckoutCallback().onSuccess(response())

        val loaded = Uri.parse(shadowOf(fragment.webView).lastLoadedUrl)
        assertEquals("abc", loaded.getQueryParameter("token"))
        assertEquals("sc_1", loaded.getQueryParameter(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM))
        assertEquals(challenge, s256(loaded.getQueryParameter(CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM)!!))
        assertNull(AffirmPlugins.get().consumeCodeVerifier())
    }

    @Test
    fun checkoutWithoutSessionCodeLoadsOriginalUrlAndKeepsVerifier() {
        prepareAuth()
        val fragment = launchCheckout(sessionCode = null)

        fragment.innerCheckoutCallback().onSuccess(response())

        assertEquals(TEST_REDIRECT_URL, shadowOf(fragment.webView).lastLoadedUrl)
        assertNotNull(AffirmPlugins.get().consumeCodeVerifier())
    }

    @Test
    fun checkoutWithoutVerifierLoadsOriginalUrl() {
        val fragment = launchCheckout(sessionCode = "sc_1")

        fragment.innerCheckoutCallback().onSuccess(response())

        assertEquals(TEST_REDIRECT_URL, shadowOf(fragment.webView).lastLoadedUrl)
    }

    @Test
    fun vcnCheckoutEmbedsDecoratedUrlInHtml() {
        val challenge = prepareAuth()
        val fragment = launchVcnCheckout(sessionCode = "sc_1")

        fragment.innerCheckoutCallback().onSuccess(response())

        val load = shadowOf(fragment.webView).lastLoadDataWithBaseURL
        assertEquals("https://sandbox.affirm.com", load.baseUrl)
        val verifier = Regex("$CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM=([A-Za-z0-9_-]+)").find(load.data)!!.groupValues[1]
        assertEquals(challenge, s256(verifier))
        assertTrue(load.data.contains("$CONNECTED_ACCOUNTS_SESSION_CODE_PARAM=sc_1"))
        assertNull(AffirmPlugins.get().consumeCodeVerifier())
    }

    @Test
    fun vcnCheckoutWithoutSessionCodeEmbedsOriginalUrl() {
        prepareAuth()
        val fragment = launchVcnCheckout(sessionCode = null)

        fragment.innerCheckoutCallback().onSuccess(response())

        val data = shadowOf(fragment.webView).lastLoadDataWithBaseURL.data
        assertTrue(data.contains(TEST_REDIRECT_URL))
        assertFalse(data.contains(CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM))
        assertNotNull(AffirmPlugins.get().consumeCodeVerifier())
    }

    @Test
    fun rotationDoesNotReuseSessionCode() {
        prepareAuth()
        val controller = Robolectric.buildActivity(
            CheckoutActivity::class.java, checkoutIntent(CheckoutActivity::class.java, "sc_1")
        ).setup()
        val fragment = controller.get().checkoutFragment()
        fragment.innerCheckoutCallback().onSuccess(response())
        assertNotNull(Uri.parse(shadowOf(fragment.webView).lastLoadedUrl)
            .getQueryParameter(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM))

        controller.recreate()
        val retained = controller.get().checkoutFragment()
        assertSame(fragment, retained)
        prepareAuth()
        retained.innerCheckoutCallback().onSuccess(response())

        assertEquals(TEST_REDIRECT_URL, shadowOf(retained.webView).lastLoadedUrl)
        assertNotNull(AffirmPlugins.get().consumeCodeVerifier())
    }

    @Test
    fun vcnActivitySavesAndRestoresSessionCode() {
        val controller = Robolectric.buildActivity(
            VcnCheckoutActivity::class.java, checkoutIntent(VcnCheckoutActivity::class.java, "sc_1")
        ).setup()
        val saved = Bundle()
        controller.saveInstanceState(saved)
        assertEquals("sc_1", saved.getString(CHECKOUT_SESSION_CODE))

        val restored = Robolectric.buildActivity(
            VcnCheckoutActivity::class.java, checkoutIntent(VcnCheckoutActivity::class.java, null)
        ).setup(saved).get()
        assertEquals("sc_1", restored.sessionCode)
        val fragment = restored.supportFragmentManager.fragments
            .filterIsInstance<VcnCheckoutFragment>().single()
        assertEquals("sc_1", fragment.requireArguments().getString(CHECKOUT_SESSION_CODE))
    }

    @Test
    fun restoredFromSavedStateDoesNotReuseSessionCode() {
        prepareAuth()
        val controller = Robolectric.buildActivity(
            CheckoutActivity::class.java, checkoutIntent(CheckoutActivity::class.java, "sc_1")
        ).setup()
        controller.get().checkoutFragment().innerCheckoutCallback().onSuccess(response())

        val saved = Bundle()
        controller.saveInstanceState(saved)
        assertEquals("sc_1", saved.getString(CHECKOUT_SESSION_CODE))

        val restored = Robolectric.buildActivity(
            CheckoutActivity::class.java, checkoutIntent(CheckoutActivity::class.java, null)
        ).setup(saved).get()
        assertEquals("sc_1", restored.sessionCode)
        val fragment = restored.checkoutFragment()
        assertEquals("sc_1", fragment.requireArguments().getString(CHECKOUT_SESSION_CODE))

        prepareAuth()
        fragment.innerCheckoutCallback().onSuccess(response())
        assertEquals(TEST_REDIRECT_URL, shadowOf(fragment.webView).lastLoadedUrl)
        assertNotNull(AffirmPlugins.get().consumeCodeVerifier())
    }

    @Test
    fun activityEntryPointsPassSessionCodeToIntent() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val checkout = CheckoutFactory.create()

        Affirm.startCheckout(host, checkout, null, -1, false, "sc_1")
        assertStarted(shadowOf(host).nextStartedActivity, CheckoutActivity::class.java, "sc_1")

        Affirm.startCheckout(host, checkout, null, -1, true, "sc_2")
        assertStarted(shadowOf(host).nextStartedActivity, VcnCheckoutActivity::class.java, "sc_2")

        Affirm.startCheckout(host, checkout, null, -1, false)
        assertStarted(shadowOf(host).nextStartedActivity, CheckoutActivity::class.java, null)

        Affirm.startCheckout(host, checkout, true)
        assertStarted(shadowOf(host).nextStartedActivity, VcnCheckoutActivity::class.java, null)
    }

    @Test
    fun fragmentEntryPointsPassSessionCodeToIntent() {
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val fragment = Fragment()
        host.supportFragmentManager.beginTransaction().add(fragment, "host").commitNow()
        val checkout = CheckoutFactory.create()

        Affirm.startCheckout(fragment, checkout, null, -1, false, "sc_1")
        assertStarted(shadowOf(host).nextStartedActivity, CheckoutActivity::class.java, "sc_1")

        Affirm.startCheckout(fragment, checkout, null, -1, true, "sc_2")
        assertStarted(shadowOf(host).nextStartedActivity, VcnCheckoutActivity::class.java, "sc_2")

        Affirm.startCheckout(fragment, checkout, null, -1, false)
        assertStarted(shadowOf(host).nextStartedActivity, CheckoutActivity::class.java, null)
    }

    @Test
    fun embeddedEntryPointsPassSessionCodeToFragment() {
        val checkout = CheckoutFactory.create()

        val withCode = Affirm.startCheckout(
            appCompatHost(), android.R.id.content, checkout, null, -1, false, "sc_1"
        )
        assertTrue(withCode is CheckoutFragment)
        assertEquals("sc_1", withCode.requireArguments().getString(CHECKOUT_SESSION_CODE))

        val vcn = Affirm.startCheckout(
            appCompatHost(), android.R.id.content, checkout, null, -1, true, "sc_2"
        )
        assertTrue(vcn is VcnCheckoutFragment)
        assertEquals("sc_2", vcn.requireArguments().getString(CHECKOUT_SESSION_CODE))

        val legacy = Affirm.startCheckout(
            appCompatHost(), android.R.id.content, checkout, null, -1, false
        )
        assertNull(legacy.requireArguments().getString(CHECKOUT_SESSION_CODE))
    }

    @Test
    fun fragmentHostedEmbeddedEntryPointsPassSessionCodeToFragment() {
        val checkout = CheckoutFactory.create()

        val withCode = Affirm.startCheckout(
            containerFragment(), ContainerFragment.CONTAINER_ID, checkout, null, -1, false, "sc_1"
        )
        assertTrue(withCode is CheckoutFragment)
        assertEquals("sc_1", withCode.requireArguments().getString(CHECKOUT_SESSION_CODE))

        val vcn = Affirm.startCheckout(
            containerFragment(), ContainerFragment.CONTAINER_ID, checkout, null, -1, true, "sc_2"
        )
        assertTrue(vcn is VcnCheckoutFragment)
        assertEquals("sc_2", vcn.requireArguments().getString(CHECKOUT_SESSION_CODE))

        val legacy = Affirm.startCheckout(
            containerFragment(), ContainerFragment.CONTAINER_ID, checkout, null, -1, false
        )
        assertNull(legacy.requireArguments().getString(CHECKOUT_SESSION_CODE))
    }

    private fun containerFragment(): Fragment {
        val fragment = ContainerFragment()
        appCompatHost().supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment).commitNow()
        return fragment
    }

    private fun prepareAuth(): String {
        var result: AffirmPrepareAuthResult? = null
        Affirm.prepareAuth { result = it }
        return result!!.codeChallenge!!
    }

    private fun launchCheckout(sessionCode: String?): CheckoutFragment =
        Robolectric.buildActivity(
            CheckoutActivity::class.java,
            checkoutIntent(CheckoutActivity::class.java, sessionCode)
        ).setup().get().checkoutFragment()

    private fun launchVcnCheckout(sessionCode: String?): VcnCheckoutFragment =
        Robolectric.buildActivity(
            VcnCheckoutActivity::class.java,
            checkoutIntent(VcnCheckoutActivity::class.java, sessionCode)
        ).setup().get().supportFragmentManager.fragments
            .filterIsInstance<VcnCheckoutFragment>().single()

    private fun CheckoutActivity.checkoutFragment(): CheckoutFragment =
        supportFragmentManager.fragments.filterIsInstance<CheckoutFragment>().single()

    private fun checkoutIntent(target: Class<*>, sessionCode: String?): Intent =
        Intent(RuntimeEnvironment.getApplication(), target)
            .putExtra(CHECKOUT_EXTRA, CheckoutFactory.create())
            .putExtra(CHECKOUT_SESSION_CODE, sessionCode)

    private fun appCompatHost(): AppCompatActivity {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        controller.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light)
        return controller.setup().get()
    }

    private fun response(): CheckoutResponse =
        CheckoutResponse.builder().setRedirectUrl(TEST_REDIRECT_URL).build()

    private fun assertStarted(intent: Intent, target: Class<*>, sessionCode: String?) {
        assertEquals(target.name, intent.component!!.className)
        assertEquals(sessionCode, intent.getStringExtra(CHECKOUT_SESSION_CODE))
    }
}

class ContainerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FrameLayout(requireContext()).apply { id = CONTAINER_ID }

    companion object {
        val CONTAINER_ID = View.generateViewId()
    }
}
