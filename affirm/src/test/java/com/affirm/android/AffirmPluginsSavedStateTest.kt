package com.affirm.android

import android.os.Bundle
import android.os.Parcel
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AffirmPluginsSavedStateTest {

    @Before
    fun setup() {
        AffirmPlugins.reset()
    }

    @After
    fun tearDown() {
        AffirmPlugins.reset()
        resetRequestCodes()
        AffirmLog.setLogLevel(Affirm.LOG_LEVEL_NONE)
        Affirm.initialize(
            Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX).build()
        )
    }

    @Test
    fun initialize_afterRestore_replacesRestoredConfiguration() {
        Affirm.initialize(
            Affirm.Configuration.Builder("saved_key", Affirm.Environment.SANDBOX).build()
        )
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        AffirmPlugins.reset()
        AffirmPlugins.restoreIfNeeded(outState)
        Affirm.initialize(
            Affirm.Configuration.Builder("app_key", Affirm.Environment.PRODUCTION).build()
        )

        assertThat(AffirmPlugins.get().publicKey()).isEqualTo("app_key")
        assertThat(AffirmPlugins.get().environment()).isEqualTo(Affirm.Environment.PRODUCTION)
        assertThat(AffirmPlugins.isRestored()).isFalse()
    }

    @Test
    fun initialize_whenInitializedByApp_keepsFirstConfiguration() {
        Affirm.initialize(
            Affirm.Configuration.Builder("first_key", Affirm.Environment.SANDBOX).build()
        )
        Affirm.initialize(
            Affirm.Configuration.Builder("second_key", Affirm.Environment.SANDBOX).build()
        )

        assertThat(AffirmPlugins.get().publicKey()).isEqualTo("first_key")
    }

    @Test
    fun restoreIfNeeded_restoresLogLevel() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX)
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .build()
        )
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        AffirmPlugins.reset()
        AffirmLog.setLogLevel(Affirm.LOG_LEVEL_NONE)
        AffirmPlugins.restoreIfNeeded(outState)

        assertThat(Affirm.getLogLevel()).isEqualTo(Affirm.LOG_LEVEL_DEBUG)
    }

    @Test
    fun restoreIfNeeded_withCorruptedState_keepsRequestCodes() {
        Affirm.Configuration.Builder("unused").setCheckoutRequestCode(1001)

        AffirmPlugins.restoreIfNeeded(savedState {
            putString("public_key", "public_key")
            putString("environment", "NOT_AN_ENVIRONMENT")
            putInt("checkout_request", 2002)
        })

        assertThat(Affirm.getCheckoutRequest()).isEqualTo(1001)
    }

    @Test
    fun restoreIfNeeded_restoresRequestCodes() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX)
                .setCheckoutRequestCode(1001)
                .setVcnCheckoutRequestCode(1002)
                .setPrequalRequestCode(1003)
                .setReceiveReasonCodes("true")
                .build()
        )
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        AffirmPlugins.reset()
        resetRequestCodes()
        AffirmPlugins.restoreIfNeeded(outState)

        assertThat(Affirm.getCheckoutRequest()).isEqualTo(1001)
        assertThat(Affirm.getVcnCheckoutRequest()).isEqualTo(1002)
        assertThat(Affirm.getPrequalRequest()).isEqualTo(1003)
        assertThat(Affirm.getReceiveReasonCodes()).isEqualTo("true")
    }

    @Test
    fun restoreIfNeeded_whenInitialized_keepsAppRequestCodes() {
        Affirm.initialize(
            Affirm.Configuration.Builder("saved_key", Affirm.Environment.SANDBOX)
                .setCheckoutRequestCode(1001)
                .build()
        )
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        AffirmPlugins.reset()
        Affirm.initialize(
            Affirm.Configuration.Builder("app_key", Affirm.Environment.SANDBOX)
                .setCheckoutRequestCode(2001)
                .build()
        )
        AffirmPlugins.restoreIfNeeded(outState)

        assertThat(Affirm.getCheckoutRequest()).isEqualTo(2001)
    }

    private fun resetRequestCodes() {
        Affirm.Configuration.Builder("unused")
            .setCheckoutRequestCode(0)
            .setVcnCheckoutRequestCode(0)
            .setPrequalRequestCode(0)
            .setReceiveReasonCodes(null)
    }

    @Test
    fun restoreIfNeeded_restoresConfiguration() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.PRODUCTION)
                .setMerchantName("Merchant")
                .setCardTip("tip")
                .setLocale("en_CA")
                .setCountryCode("CAN")
                .setUseLegacyNativeRendering(true)
                .build()
        )
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        AffirmPlugins.reset()
        AffirmPlugins.restoreIfNeeded(outState)

        val plugins = AffirmPlugins.get()
        assertThat(plugins).isNotNull()
        assertThat(plugins.publicKey()).isEqualTo("public_key")
        assertThat(plugins.environment()).isEqualTo(Affirm.Environment.PRODUCTION)
        assertThat(plugins.merchantName()).isEqualTo("Merchant")
        assertThat(plugins.cardTip()).isEqualTo("tip")
        assertThat(plugins.locale()).isEqualTo("en_CA")
        assertThat(plugins.countryCode()).isEqualTo("CAN")
        assertThat(plugins.useLegacyNativeRendering()).isTrue()
    }

    @Test
    fun restoreIfNeeded_withoutOptionalFields_restoresDefaults() {
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX).build()
        )
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        AffirmPlugins.reset()
        AffirmPlugins.restoreIfNeeded(outState)

        val plugins = AffirmPlugins.get()
        assertThat(plugins.merchantName()).isNull()
        assertThat(plugins.cardTip()).isNull()
        assertThat(plugins.locale()).isEqualTo("en_US")
        assertThat(plugins.countryCode()).isEqualTo("USA")
        assertThat(plugins.useLegacyNativeRendering()).isFalse()
    }

    @Test
    fun restoreIfNeeded_keepsExistingInstance() {
        Affirm.initialize(
            Affirm.Configuration.Builder("saved_key", Affirm.Environment.SANDBOX).build()
        )
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        AffirmPlugins.reset()
        Affirm.initialize(
            Affirm.Configuration.Builder("app_key", Affirm.Environment.SANDBOX).build()
        )
        AffirmPlugins.restoreIfNeeded(outState)

        assertThat(AffirmPlugins.get().publicKey()).isEqualTo("app_key")
    }

    @Test
    fun restoreIfNeeded_withoutSavedState_staysUninitialized() {
        AffirmPlugins.restoreIfNeeded(null)
        AffirmPlugins.restoreIfNeeded(Bundle())

        assertThat(AffirmPlugins.get()).isNull()
    }

    @Test
    fun restoreIfNeeded_withCorruptedState_staysUninitialized() {
        AffirmPlugins.restoreIfNeeded(savedState {
            putString("public_key", "public_key")
            putString("environment", "NOT_AN_ENVIRONMENT")
        })
        AffirmPlugins.restoreIfNeeded(savedState {
            putInt("public_key", 1)
            putString("environment", Affirm.Environment.SANDBOX.name)
        })
        AffirmPlugins.restoreIfNeeded(savedState {
            putString("environment", Affirm.Environment.SANDBOX.name)
        })
        AffirmPlugins.restoreIfNeeded(savedState {
            putInt("checkout_request", 2002)
        })

        assertThat(AffirmPlugins.get()).isNull()
    }

    @Test
    fun saveState_whenUninitialized_writesNothing() {
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        assertThat(outState.isEmpty).isTrue()
    }

    @Test
    fun restoreIfNeeded_restoresEveryConfigurationField() {
        val original =
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX).build()
        val fields = Affirm.Configuration::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .onEach { it.isAccessible = true }
        fields.forEach { it.set(original, nonDefaultValue(it, it.get(original))) }
        Affirm.initialize(original)
        val outState = Bundle()
        AffirmPlugins.saveState(outState)

        AffirmPlugins.reset()
        AffirmPlugins.restoreIfNeeded(throughParcel(outState))

        val restored = AffirmPlugins.get().configuration
        assertThat(restored).isNotSameAs(original)
        fields.forEach {
            assertWithMessage(it.name).that(it.get(restored)).isEqualTo(it.get(original))
        }
    }

    private fun nonDefaultValue(field: Field, current: Any?): Any = when (field.type) {
        String::class.java -> "${field.name}_value"
        Boolean::class.javaPrimitiveType -> !(current as Boolean)
        Affirm.Environment::class.java -> Affirm.Environment.values().first { it != current }
        else -> throw AssertionError(
            "Add a non-default value for ${field.type} (${field.name}) to this test"
        )
    }

    private fun throughParcel(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(bundle)
            parcel.setDataPosition(0)
            return parcel.readBundle(javaClass.classLoader)!!
        } finally {
            parcel.recycle()
        }
    }

    private fun savedState(state: Bundle.() -> Unit): Bundle = Bundle().apply {
        putBundle("com.affirm.android.AffirmPlugins.state", Bundle().apply(state))
    }
}
