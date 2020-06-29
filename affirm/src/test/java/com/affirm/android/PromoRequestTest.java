package com.affirm.android;

import android.text.SpannableString;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.Item;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

        final List<Item> items = new ArrayList<>();
        items.add(Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl(
                        "http://www.m2motorsportinc.com/media/catalog/product/cache/1/thumbnail" +
                                "/9df78eab33525d08d6e5fb8d27136e95/v/e/velocity-vw125-wheels-rims.jpg")
                .setQty(1)
                .setSku("wheel")
                .setUnitPrice(BigDecimal.valueOf(1000.0))
                .setUrl("http://merchant.com/great_deal_wheel")
                .build()
        );

        final PromoRequest affirmPromoRequest =
                new PromoRequest(null, null, BigDecimal.valueOf(1100.0), false, AffirmColor.AFFIRM_COLOR_TYPE_BLUE, AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, true, items, callback);
        affirmPromoRequest.create();
    }


    @Test
    public void testPromo() {
        Affirm.PromoRequestData requestData = new Affirm.PromoRequestData.Builder(BigDecimal.valueOf(1100.0), true)
                .setPageType(null)
                .build();


        final AffirmRequest promoRequest = Affirm.fetchPromotion(requestData, 16, RuntimeEnvironment.application, new PromotionCallback() {
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
