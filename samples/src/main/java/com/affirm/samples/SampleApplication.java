package com.affirm.samples;

import android.app.Application;

import com.affirm.android.Affirm;
import com.squareup.leakcanary.LeakCanary;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        LeakCanary.install(this);

        Affirm.initialize(new Affirm.Configuration.Builder()
                .setEnvironment(Affirm.Environment.SANDBOX)
                .setPublicKey("Y8CQXFF044903JC0")
                .setName("")
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .build()
        );
    }
}
