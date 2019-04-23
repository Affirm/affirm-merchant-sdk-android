package com.affirm.samples;

import android.app.Application;

import com.affirm.android.Affirm;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Affirm.initialize(new Affirm.Configuration.Builder("F6B2V0K9D5I8033Y", Affirm.Environment.SANDBOX)
                .setMerchantName(null)
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .build()
        );
    }
}
