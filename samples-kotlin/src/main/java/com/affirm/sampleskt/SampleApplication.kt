package com.affirm.sampleskt

import android.app.Application

import com.affirm.android.Affirm
import com.squareup.leakcanary.LeakCanary

class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        LeakCanary.install(this)

        Affirm.initialize(Affirm.Configuration.Builder()
                .setEnvironment(Affirm.Environment.SANDBOX)
                .setPublicKey("Y8CQXFF044903JC0")
                .setName(null)
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .build()
        )
    }
}
