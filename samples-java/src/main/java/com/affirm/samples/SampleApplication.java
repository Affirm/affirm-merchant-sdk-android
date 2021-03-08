package com.affirm.samples;

import android.app.Application;

import com.affirm.android.Affirm;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Affirm.initialize(new Affirm.Configuration.Builder("6YN1HYTDBEMFF8CK") // In Canadian, should use Canada public API key
                .setEnvironment(Affirm.Environment.SANDBOX)
                .setMerchantName(null)
                .setReceiveReasonCodes("true")
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .setCheckoutRequestCode(8001)
                .setVcnCheckoutRequestCode(8002)
                .setPrequalRequestCode(8003)
                .setLocation(Affirm.Location.US)  // "CA" for Canadian, "US" for American (If not set, default will use US)
                .setMerchantName("Merchant Name")
                .setCardTip("We've added these card details to Rakuten Autofill for quick, easy checkout.")
                .build()
        );
    }
}
