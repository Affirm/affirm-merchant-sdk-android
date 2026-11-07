package com.affirm.sampleskt

import android.app.Application
import com.affirm.android.Affirm
import java.util.*

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Affirm.initialize(Affirm.Configuration.Builder("Y8CQXFF044903JC0")
                .setEnvironment(Affirm.Environment.SANDBOX)
                .setCountryCode(Locale.US.isO3Country)  // Default USA
                .setLocale(Locale.US.toString())    // Default en_US
                .setMerchantName(null)
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                // true keeps promo views native even when Affirm migrates them to HTML.
                .setUseLegacyNativeRendering(false)
                .build()
        )
    }
}
