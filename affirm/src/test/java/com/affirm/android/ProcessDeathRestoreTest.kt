package com.affirm.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.affirm.android.AffirmConstants.AFFIRM_NOT_INITIALIZED_MESSAGE
import com.affirm.android.AffirmConstants.AMOUNT
import com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW
import com.affirm.android.AffirmConstants.CHECKOUT_ERROR
import com.affirm.android.AffirmConstants.CHECKOUT_EXTRA
import com.affirm.android.AffirmConstants.TYPE_EXTRA
import com.affirm.android.AffirmConstants.VCN_CARD_DETAILS_UNAVAILABLE_MESSAGE
import com.affirm.android.model.CardDetails
import com.affirm.android.model.CardDetailsInner
import com.affirm.android.model.VcnReason
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowActivity
import java.math.BigDecimal
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProcessDeathRestoreTest {

    @Before
    fun setup() {
        AffirmPlugins.reset()
    }

    @After
    fun tearDown() {
        AffirmPlugins.reset()
        resetRequestCodes()
        Affirm.initialize(
            Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX).build()
        )
    }

    @Test
    fun prequalActivity_restoredAfterProcessDeath_restoresConfiguration() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX).build()
        )
        val savedState = Bundle()
        Robolectric.buildActivity(PrequalActivity::class.java, prequalIntent())
            .setup()
            .saveInstanceState(savedState)
            .pause()
            .stop()
            .destroy()

        AffirmPlugins.reset()
        val controller = Robolectric.buildActivity(PrequalActivity::class.java, prequalIntent())
            .setup(savedState)

        assertThat(AffirmPlugins.get()).isNotNull()
        assertThat(AffirmPlugins.get().publicKey()).isEqualTo("public_key")
        assertThat(controller.get().isFinishing).isFalse()

        controller.pause().stop().destroy()
    }

    @Test
    fun modalActivity_restoredAfterProcessDeath_restoresConfiguration() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX).build()
        )
        val intent = Intent(RuntimeEnvironment.getApplication(), ModalActivity::class.java)
            .putExtra(AMOUNT, BigDecimal.valueOf(100))
            .putExtra(TYPE_EXTRA, ModalFragment.ModalType.PRODUCT)
        val savedState = Bundle()
        Robolectric.buildActivity(ModalActivity::class.java, intent)
            .setup()
            .saveInstanceState(savedState)
            .pause()
            .stop()
            .destroy()

        AffirmPlugins.reset()
        val controller = Robolectric.buildActivity(ModalActivity::class.java, intent)
            .setup(savedState)

        assertThat(AffirmPlugins.get()).isNotNull()
        assertThat(AffirmPlugins.get().publicKey()).isEqualTo("public_key")
        assertThat(controller.get().isFinishing).isFalse()

        controller.pause().stop().destroy()
    }

    @Test
    fun checkoutActivity_restoredAfterProcessDeath_restoresConfigurationAndRequestCode() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX)
                .setCheckoutRequestCode(1001)
                .build()
        )
        val savedState = Bundle()
        Robolectric.buildActivity(CheckoutActivity::class.java, checkoutIntent())
            .setup()
            .saveInstanceState(savedState)
            .pause()
            .stop()
            .destroy()

        AffirmPlugins.reset()
        resetRequestCodes()
        val controller = Robolectric.buildActivity(CheckoutActivity::class.java, checkoutIntent())
            .setup(savedState)

        assertThat(AffirmPlugins.get().publicKey()).isEqualTo("public_key")
        assertThat(Affirm.getCheckoutRequest()).isEqualTo(1001)
        assertThat(controller.get().isFinishing).isFalse()

        controller.pause().stop().destroy()
    }

    @Test
    fun vcnCheckoutActivity_restoredAfterProcessDeath_restoresConfigurationAndRequestCode() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX)
                .setVcnCheckoutRequestCode(1002)
                .build()
        )
        val intent = Intent(RuntimeEnvironment.getApplication(), VcnCheckoutActivity::class.java)
            .putExtra(CHECKOUT_EXTRA, CheckoutFactory.create())
            .putExtra(CHECKOUT_CARD_AUTH_WINDOW, -1)
        val savedState = Bundle()
        Robolectric.buildActivity(VcnCheckoutActivity::class.java, intent)
            .setup()
            .saveInstanceState(savedState)
            .pause()
            .stop()
            .destroy()

        AffirmPlugins.reset()
        resetRequestCodes()
        val controller = Robolectric.buildActivity(VcnCheckoutActivity::class.java, intent)
            .setup(savedState)

        assertThat(AffirmPlugins.get().publicKey()).isEqualTo("public_key")
        assertThat(Affirm.getVcnCheckoutRequest()).isEqualTo(1002)
        assertThat(controller.get().isFinishing).isFalse()

        controller.pause().stop().destroy()
    }

    @Test
    fun vcnDisplayActivity_restoredAfterProcessDeath_routesErrorToCustomRequestCode() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX)
                .setVcnCheckoutRequestCode(8002)
                .build()
        )
        AffirmPlugins.get().setCacheCardDetails(CardDetailsInner(cardDetails(), Date()))
        val savedState = Bundle()
        Robolectric.buildActivity(VcnDisplayActivity::class.java, vcnDisplayIntent())
            .setup()
            .saveInstanceState(savedState)
            .pause()
            .stop()
            .destroy()

        AffirmPlugins.reset()
        resetRequestCodes()
        val activity = Robolectric.buildActivity(VcnDisplayActivity::class.java, vcnDisplayIntent())
            .setup(savedState)
            .get()

        assertThat(Affirm.getVcnCheckoutRequest()).isEqualTo(8002)
        assertThat(activity.isFinishing).isTrue()
        assertThat(shadow(activity).resultCode).isEqualTo(Affirm.RESULT_ERROR)

        val callbacks = RecordingVcnCheckoutCallbacks()
        val handled = Affirm.handleVcnCheckoutData(
            callbacks, 8002, shadow(activity).resultCode, shadow(activity).resultIntent
        )
        assertThat(handled).isTrue()
        assertThat(callbacks.error).isEqualTo(VCN_CARD_DETAILS_UNAVAILABLE_MESSAGE)
    }

    @Test
    fun vcnDisplayActivity_withoutConfiguration_finishesWithError() {
        val activity = Robolectric.buildActivity(VcnDisplayActivity::class.java, vcnDisplayIntent())
            .setup()
            .get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadow(activity).resultCode).isEqualTo(Affirm.RESULT_ERROR)
        assertThat(shadow(activity).resultIntent.getStringExtra(CHECKOUT_ERROR))
            .isEqualTo(AFFIRM_NOT_INITIALIZED_MESSAGE)
    }

    @Test
    fun vcnDisplayActivity_withoutCachedCardDetails_finishesWithError() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX).build()
        )
        val activity = Robolectric.buildActivity(VcnDisplayActivity::class.java, vcnDisplayIntent())
            .setup()
            .get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadow(activity).resultCode).isEqualTo(Affirm.RESULT_ERROR)
        assertThat(shadow(activity).resultIntent.getStringExtra(CHECKOUT_ERROR))
            .isEqualTo(VCN_CARD_DETAILS_UNAVAILABLE_MESSAGE)
    }

    @Test
    fun prequalActivity_withoutConfiguration_finishesWithError() {
        val activity = Robolectric.buildActivity(PrequalActivity::class.java, prequalIntent())
            .setup()
            .get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadow(activity).resultCode).isEqualTo(Affirm.RESULT_ERROR)
    }

    @Test
    fun checkoutActivity_withoutConfiguration_finishesWithErrorAndDestroysCleanly() {
        val controller =
            Robolectric.buildActivity(CheckoutActivity::class.java, checkoutIntent()).setup()
        val activity = controller.get()

        assertThat(activity.isFinishing).isTrue()
        assertThat(shadow(activity).resultCode).isEqualTo(Affirm.RESULT_ERROR)
        assertThat(shadow(activity).resultIntent.getStringExtra(CHECKOUT_ERROR))
            .isEqualTo(AFFIRM_NOT_INITIALIZED_MESSAGE)

        controller.pause().stop().destroy()
    }

    @Test
    fun embeddedCheckoutFragment_withoutConfiguration_reportsErrorToHost() {
        val host = Robolectric.buildActivity(EmbeddedCheckoutHost::class.java).setup().get()
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(host.error).isEqualTo(AFFIRM_NOT_INITIALIZED_MESSAGE)
        assertThat(host.supportFragmentManager.fragments).isEmpty()
    }

    private fun shadow(activity: Activity): ShadowActivity = Shadow.extract(activity)

    private fun prequalIntent(): Intent =
        Intent(RuntimeEnvironment.getApplication(), PrequalActivity::class.java)
            .putExtra(AMOUNT, BigDecimal.valueOf(100))

    private fun checkoutIntent(): Intent =
        Intent(RuntimeEnvironment.getApplication(), CheckoutActivity::class.java)
            .putExtra(CHECKOUT_EXTRA, CheckoutFactory.create())
            .putExtra(CHECKOUT_CARD_AUTH_WINDOW, -1)

    private fun vcnDisplayIntent(): Intent =
        Intent(RuntimeEnvironment.getApplication(), VcnDisplayActivity::class.java)
            .putExtra(CHECKOUT_EXTRA, CheckoutFactory.create())

    private fun cardDetails(): CardDetails = CardDetails.builder()
        .setCardholderName("John Smith")
        .setCheckoutToken("checkout_token")
        .setCvv("123")
        .setNumber("4012888888881881")
        .setExpiration("0719")
        .build()

    private fun resetRequestCodes() {
        Affirm.Configuration.Builder("unused")
            .setCheckoutRequestCode(0)
            .setVcnCheckoutRequestCode(0)
            .setPrequalRequestCode(0)
    }

    private class RecordingVcnCheckoutCallbacks : Affirm.VcnCheckoutCallbacks {
        var error: String? = null

        override fun onAffirmVcnCheckoutError(message: String?) {
            error = message
        }

        override fun onAffirmVcnCheckoutCancelled() = Unit

        override fun onAffirmVcnCheckoutCancelledReason(vcnReason: VcnReason) = Unit

        override fun onAffirmVcnCheckoutSuccess(cardDetails: CardDetails) = Unit
    }

    class EmbeddedCheckoutHost : AppCompatActivity(), Affirm.CheckoutCallbacks {
        var error: String? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.AffirmDefaultTheme)
            super.onCreate(savedInstanceState)
            Affirm.startCheckout(
                this, android.R.id.content, CheckoutFactory.create(), null, -1, false
            )
        }

        override fun onAffirmCheckoutError(message: String?) {
            error = message
        }

        override fun onAffirmCheckoutCancelled() = Unit

        override fun onAffirmCheckoutSuccess(token: String) = Unit
    }
}
