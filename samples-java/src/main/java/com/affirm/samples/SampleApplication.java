package com.affirm.samples;

import android.app.Application;

import com.affirm.android.Affirm;

import java.util.Locale;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Affirm.initialize(new Affirm.Configuration.Builder(Config.PUBLIC_KEY)
                .setEnvironment(Affirm.Environment.SANDBOX)
                .setCountryCode("USA")  // Default USA
                .setLocale(Locale.US.toString())    // Default en_US
                .setMerchantName(null)
                .setReceiveReasonCodes("true")
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .setCheckoutRequestCode(8001)
                .setVcnCheckoutRequestCode(8002)
                .setPrequalRequestCode(8003)
                .setMerchantName("Merchant Name")
                .setCardTip("We've added these card details to Rakuten Autofill for quick, easy checkout.")
                .build()
        );
    }
}
