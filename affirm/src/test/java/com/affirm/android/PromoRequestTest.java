package com.affirm.android;

import android.app.Application;
import android.content.Context;
import android.text.SpannableString;

import com.affirm.android.exception.AffirmException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
                new PromoRequest(null, null, 1100, false, AffirmColor.AFFIRM_COLOR_TYPE_BLUE, AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, true, callback);
        affirmPromoRequest.create();
    }


    @Test
    public void testPromo() {
        Affirm.PromoRequestData requestDate = new Affirm.PromoRequestData.Builder(1100, true)
                .setPromoId(null)
                .setPageType(null)
                .build();


        final AffirmRequest promoRequest = Affirm.fetchPromotion(requestDate, 16, RuntimeEnvironment.application, new PromotionCallback() {
            @Override
            public void onSuccess(@Nullable SpannableString spannableString, boolean showPrequal) {
                assertNotNull(spannableString);
            }

            @Override
            public void onFailure(@NonNull AffirmException exception) {
            }
        });

        promoRequest.create();
    }

}
