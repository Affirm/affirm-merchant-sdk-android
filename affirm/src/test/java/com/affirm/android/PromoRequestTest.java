package com.affirm.android;

import com.affirm.android.model.Item;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class PromoRequestTest {

    private static final String expectedPromoUrl =
            "https://sandbox.affirm.com/api/promos/v2/Y8CQXFF044903JC0?is_sdk=true&field=ala&amount=110000&show_cta=false&logo_color=blue&logo_type=logo&items=%255B%257B%2522display_name%2522%253A%2522Great%2520Deal%2520Wheel%2522%252C%2522sku%2522%253A%2522wheel%2522%252C%2522unit_price%2522%253A100000%252C%2522qty%2522%253A1%252C%2522item_url%2522%253A%2522http%253A%252F%252Fmerchant.com%252Fgreat_deal_wheel%2522%252C%2522item_image_url%2522%253A%2522http%253A%252F%252Fwww.m2motorsportinc.com%252Fmedia%252Fcatalog%252Fproduct%252Fcache%252F1%252Fthumbnail%252F9df78eab33525d08d6e5fb8d27136e95%252Fv%252Fe%252Fvelocity-vw125-wheels-rims.jpg%2522%257D%255D&locale=en_US";

    @Before
    public void setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(new Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            );
        }
    }

    @Test
    public void testGetNewPromo() {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);

        Mockito.when(client.newCall(any(Request.class))).thenReturn(call);

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
                new PromoRequest(client, null, null, BigDecimal.valueOf(1100.0), false, AffirmColor.AFFIRM_COLOR_TYPE_BLUE, AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, true, items, callback);
        affirmPromoRequest.create();

        Mockito.verify(client).newCall(requestCaptor.capture());
        Request request = requestCaptor.getValue();
        Truth.assertThat(request.url().toString().equals(expectedPromoUrl)).isTrue();
    }
}