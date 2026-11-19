package com.affirm.android

import android.content.Intent
import com.affirm.android.AffirmConstants.AFFIRM_NOT_INITIALIZED_MESSAGE
import com.affirm.android.AffirmConstants.AMOUNT
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
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrequalResultTest {

    private var error: String? = null
    private val callbacks = Affirm.PrequalCallbacks { error = it }

    @Before
    fun setup() {
        AffirmPlugins.reset()
        Affirm.initialize(
            Affirm.Configuration.Builder("public_key", Affirm.Environment.SANDBOX).build()
        )
    }

    @After
    fun tearDown() {
        AffirmPlugins.reset()
        Affirm.initialize(
            Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX).build()
        )
    }

    @Test
    fun handlePrequalData_deliversPrequalActivityError() {
        val activity = Robolectric.buildActivity(PrequalActivity::class.java, prequalIntent())
            .setup()
            .get()

        activity.onAffirmPrequalError("prequal failed")
        val result = shadowOf(activity)

        val handled = Affirm.handlePrequalData(
            callbacks, Affirm.getPrequalRequest(), result.resultCode, result.resultIntent
        )

        assertThat(handled).isTrue()
        assertThat(error).isEqualTo("prequal failed")
    }

    @Test
    fun handlePrequalData_deliversNotInitializedError() {
        AffirmPlugins.reset()
        val activity = Robolectric.buildActivity(PrequalActivity::class.java, prequalIntent())
            .setup()
            .get()
        val result = shadowOf(activity)

        Affirm.handlePrequalData(
            callbacks, Affirm.getPrequalRequest(), result.resultCode, result.resultIntent
        )

        assertThat(result.resultCode).isEqualTo(Affirm.RESULT_ERROR)
        assertThat(error).isEqualTo(AFFIRM_NOT_INITIALIZED_MESSAGE)
    }

    private fun prequalIntent(): Intent =
        Intent(RuntimeEnvironment.getApplication(), PrequalActivity::class.java)
            .putExtra(AMOUNT, BigDecimal.valueOf(100))
}
