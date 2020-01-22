package com.affirm.sampleskt

import android.app.Application
import com.affirm.android.Affirm

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Affirm.initialize(Affirm.Configuration.Builder("Y8CQXFF044903JC0") // In Canadian, should use Canada public API key
                .setEnvironment(Affirm.Environment.SANDBOX)
                .setMerchantName(null)
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .setLocation(Affirm.Location.US)  // "CA" for Canadian, "US" for American (If not set, default will use US)
                .build()
        )
    }
}
