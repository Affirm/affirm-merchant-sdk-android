package com.affirm.android;

import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;

public class AffirmPluginsTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            );
        }
    }

    @Test
    public void testBaseUrl() {
        Truth.assertThat(AffirmPlugins.get().baseUrl()).isEqualTo("sandbox.affirm.com");
    }

    @Test
    public void testBaseJsUrl() {
        Truth.assertThat(AffirmPlugins.get().baseJsUrl()).isEqualTo("cdn1-sandbox.affirm.com");
    }

    @Test
    public void testPublicKey() {
        Truth.assertThat(AffirmPlugins.get().publicKey()).isEqualTo("Y8CQXFF044903JC0");
    }

    @Test
    public void testEnvironment() {
        Truth.assertThat(AffirmPlugins.get().environment()).isEqualTo(Affirm.Environment.SANDBOX);
    }

    @Test
    public void testEnvironmentName() {
        Truth.assertThat(AffirmPlugins.get().environmentName()).isEqualTo("SANDBOX");
    }

    @Test
    public void testBasePromoUrl() {
        Truth.assertThat(AffirmPlugins.get().basePromoUrl()).isEqualTo("sandbox.affirm.com");
    }

    @Test
    public void testTrackerBaseUrl() {
        Truth.assertThat(AffirmPlugins.get().trackerBaseUrl()).isEqualTo("tracker.affirm.com");
    }

    @Test
    public void testBaseInvalidCheckoutRedirectUrl() {
        Truth.assertThat(AffirmPlugins.get().baseInvalidCheckoutRedirectUrl()).isEqualTo("sandbox.affirm.com/u/");
    }

    @Test
    public void testMerchantName() {
        Truth.assertThat(AffirmPlugins.get().merchantName()).isEqualTo(null);
    }
}
