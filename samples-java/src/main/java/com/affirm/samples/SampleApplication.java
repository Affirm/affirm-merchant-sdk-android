package com.affirm.samples;

import android.app.Application;

import com.affirm.android.Affirm;
import com.squareup.leakcanary.LeakCanary;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        LeakCanary.install(this);

        Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0")
                .setEnvironment(Affirm.Environment.SANDBOX)
                .setName(null)
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .build()
        );
    }
}
