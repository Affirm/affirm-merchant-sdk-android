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
        Truth.assertThat(AffirmPlugins.get().checkoutUrl()).isEqualTo("api.global-sandbox.affirm.com");
    }

    @Test
    public void testBaseJsUrl() {
        Truth.assertThat(AffirmPlugins.get().jsUrl()).isEqualTo("cdn1-sandbox.affirm.com");
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
        Truth.assertThat(AffirmPlugins.get().promoUrl()).isEqualTo("sandbox.affirm.com");
    }

    @Test
    public void testTrackerBaseUrl() {
        Truth.assertThat(AffirmPlugins.get().trackerUrl()).isEqualTo("tracker.affirm.com");
    }

    @Test
    public void testBaseInvalidCheckoutRedirectUrl() {
        Truth.assertThat(AffirmPlugins.get().invalidCheckoutRedirectUrl()).isEqualTo("sandbox.affirm.com/u/");
    }
}
