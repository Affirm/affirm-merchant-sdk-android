package com.affirm.android;

import com.affirm.android.exception.AffirmException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import androidx.annotation.NonNull;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class PromoRequestTest {

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("sdf", Affirm.Environment.SANDBOX)
                    .build()
            );
        }
    }

    @Test
    public void testGetNewPromo() {
        final SpannablePromoCallback callback = new SpannablePromoCallback() {
            @Override
            public void onPromoWritten(@NonNull final String promo, final boolean showPrequal) {
                assertNotNull(promo);
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {

            }
        };

        final PromoRequest affirmPromoRequest =
                new PromoRequest(null, null, 1100, false, callback);
        affirmPromoRequest.create();
    }

}
