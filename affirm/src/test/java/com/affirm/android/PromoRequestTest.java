package com.affirm.android;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.Item;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PromoRequestTest {

    private static final String VALID_PROMO_BODY =
            "{\"promo\":{\"ala\":\"Starting at $10/mo with {affirm_logo}\","
                    + "\"html_ala\":\"<p>Starting at $10/mo</p>\","
                    + "\"config\":{\"promo_prequal_enabled\":true,\"promo_style\":\"fast\"}}}";

    private static final String expectedPromoUrl =
            "https://sandbox.affirm.com/api/promos/v2/Y8CQXFF044903JC0?is_sdk=true&field=ala&amount=110000&show_cta=false&logo_color=blue&logo_type=logo&items=%5B%7B%22display_name%22%3A%22Great%20Deal%20Wheel%22%2C%22sku%22%3A%22wheel%22%2C%22unit_price%22%3A100000%2C%22qty%22%3A1%2C%22item_url%22%3A%22http%3A%2F%2Fmerchant.com%2Fgreat_deal_wheel%22%2C%22item_image_url%22%3A%22http%3A%2F%2Fwww.m2motorsportinc.com%2Fmedia%2Fcatalog%2Fproduct%2Fcache%2F1%2Fthumbnail%2F9df78eab33525d08d6e5fb8d27136e95%2Fv%2Fe%2Fvelocity-vw125-wheels-rims.jpg%22%7D%5D&locale=en_US";

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

    @Test
    public void deliversFailureForActiveCall() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call call = mockCall();
        PromoRequest request = newRequest(callback, call);

        request.create();
        respondWithMalformedBody(call);

        verify(callback).onFailure(any(AffirmException.class));
    }

    @Test
    public void dropsFailureAfterCancel() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call call = mockCall();
        PromoRequest request = newRequest(callback, call);

        request.create();
        request.cancel();
        respondWithMalformedBody(call);

        verify(callback, never()).onFailure(any(AffirmException.class));
    }

    @Test
    public void deliversFailureOnlyForLatestCall() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call first = mockCall();
        Call second = mockCall();
        PromoRequest request = newRequest(callback, first, second);

        request.create();
        request.create();
        respondWithMalformedBody(first);

        verify(callback, never()).onFailure(any(AffirmException.class));

        respondWithMalformedBody(second);

        verify(callback).onFailure(any(AffirmException.class));
    }

    @Test
    public void deliversSuccessForActiveCall() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call call = mockCall();
        PromoRequest request = newRequest(callback, call);

        request.create();
        respond(call, VALID_PROMO_BODY);

        verify(callback).onPromoWritten(anyString(), anyString(), anyBoolean());
    }

    @Test
    public void dropsSuccessAfterCancel() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call call = mockCall();
        PromoRequest request = newRequest(callback, call);

        request.create();
        request.cancel();
        respond(call, VALID_PROMO_BODY);

        verify(callback, never()).onPromoWritten(anyString(), anyString(), anyBoolean());
    }

    @Test
    public void deliversSuccessOnlyForLatestCall() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call first = mockCall();
        Call second = mockCall();
        PromoRequest request = newRequest(callback, first, second);

        request.create();
        request.create();
        respond(first, VALID_PROMO_BODY);

        verify(callback, never()).onPromoWritten(anyString(), anyString(), anyBoolean());

        respond(second, VALID_PROMO_BODY);

        verify(callback).onPromoWritten(anyString(), anyString(), anyBoolean());
    }

    @Test
    public void dropsSuccessCancelledWhileQueued() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call call = mockCall();
        PromoRequest request = newRequest(callback, call);

        request.create();
        deliver(call, VALID_PROMO_BODY);
        request.cancel();
        ShadowLooper.idleMainLooper();

        verify(callback, never()).onPromoWritten(anyString(), anyString(), anyBoolean());
    }

    @Test
    public void dropsNetworkFailureAfterCancel() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call call = mockCall();
        PromoRequest request = newRequest(callback, call);

        request.create();
        request.cancel();
        captureCallback(call).onFailure(call, new IOException("Canceled"));
        ShadowLooper.idleMainLooper();

        verify(callback, never()).onFailure(any(AffirmException.class));
        verify(call, never()).request();
    }

    @Test
    public void deliversNetworkFailureForActiveCall() {
        SpannablePromoCallback callback = mock(SpannablePromoCallback.class);
        Call call = mockCall();
        PromoRequest request = newRequest(callback, call);

        request.create();
        captureCallback(call).onFailure(call, new IOException("timeout"));
        ShadowLooper.idleMainLooper();

        verify(callback).onFailure(any(AffirmException.class));
    }

    private static Call mockCall() {
        Call call = mock(Call.class);
        AtomicBoolean canceled = new AtomicBoolean();
        doAnswer(invocation -> {
            canceled.set(true);
            return null;
        }).when(call).cancel();
        when(call.isCanceled()).thenAnswer(invocation -> canceled.get());
        when(call.request()).thenReturn(
                new Request.Builder().url("https://sandbox.affirm.com").build());
        return call;
    }

    private PromoRequest newRequest(SpannablePromoCallback callback, Call call, Call... more) {
        OkHttpClient client = mock(OkHttpClient.class);
        Mockito.when(client.newCall(any(Request.class))).thenReturn(call, more);
        return new PromoRequest(client, null, null, BigDecimal.ONE, false,
                AffirmColor.AFFIRM_COLOR_TYPE_BLUE, AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO,
                false, null, callback);
    }

    private void respondWithMalformedBody(Call call) {
        respond(call, "not json");
    }

    private void respond(Call call, String body) {
        deliver(call, body);
        ShadowLooper.idleMainLooper();
    }

    private static Callback captureCallback(Call call) {
        ArgumentCaptor<Callback> captor = ArgumentCaptor.forClass(Callback.class);
        verify(call).enqueue(captor.capture());
        return captor.getValue();
    }

    private void deliver(Call call, String body) {
        Request request = new Request.Builder().url("https://sandbox.affirm.com").build();
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(body, MediaType.get("application/json")))
                .build();
        try {
            captureCallback(call).onResponse(call, response);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
