package com.affirm.android;

import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;

public class AffirmConstantsTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            );
        }
    }

    @Test
    public void getColorResTest() {
        Truth.assertThat(AffirmConstants.getSandboxUrl()).isEqualTo("sandbox.affirm.com");
        Truth.assertThat(AffirmConstants.getSandboxJsUrl()).isEqualTo("cdn1-sandbox.affirm.com");
        Truth.assertThat(AffirmConstants.getTrackerUrl()).isEqualTo("tracker.affirm.com");
        Truth.assertThat(AffirmConstants.getProductionUrl()).isEqualTo("api.affirm.com");
        Truth.assertThat(AffirmConstants.getProductionJsUrl()).isEqualTo("cdn1.affirm.com");
        Truth.assertThat(AffirmConstants.getStagingPromoUrl()).isEqualTo("sandbox.affirm.com");
        Truth.assertThat(AffirmConstants.getProductionPromoUrl()).isEqualTo("www.affirm.com");
        Truth.assertThat(AffirmConstants.getStagingInvalidCheckoutRedirectUrl()).isEqualTo("sandbox.affirm.com/u/");
        Truth.assertThat(AffirmConstants.getProductionInvalidCheckoutRedirectUrl()).isEqualTo("api.affirm.com/u/");
    }
}
