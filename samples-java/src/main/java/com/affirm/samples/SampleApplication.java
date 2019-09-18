package com.affirm.samples;

import android.app.Application;

import com.affirm.android.Affirm;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Affirm.initialize(new Affirm.Configuration.Builder("ZY2590ABAF62S9KM")
                .setEnvironment(Affirm.Environment.SANDBOX)
                .setMerchantName(null)
                .setReceiveReasonCodes("true")
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .setCheckoutRequestCode(8001)
                .setVcnCheckoutRequestCode(8002)
                .setPrequalRequestCode(8003)
                .build()
        );
    }
}
