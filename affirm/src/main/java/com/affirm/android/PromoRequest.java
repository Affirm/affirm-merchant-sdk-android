package com.affirm.android;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.PromoResponse;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static com.affirm.android.AffirmConstants.PROMO_PATH;
import static com.affirm.android.AffirmConstants.TAG_GET_NEW_PROMO;
import static com.affirm.android.AffirmTracker.TrackingEvent.NETWORK_ERROR;
import static com.affirm.android.AffirmTracker.TrackingLevel.ERROR;
import static com.affirm.android.AffirmTracker.createTrackingNetworkJsonObj;

class PromoRequest implements AffirmRequest {

    @Nullable
    private final String promoId;
    private final float dollarAmount;
    private final boolean showCta;
    @NonNull
    private final AffirmColor affirmColor;
    @NonNull
    private final AffirmLogoType affirmLogoType;
    @Nullable
    private final PromoPageType pageType;
    @NonNull
    private SpannablePromoCallback callback;

    private Call promoCall;

    PromoRequest(
            @Nullable final String promoId,
            @Nullable final PromoPageType pageType,
            final float dollarAmount,
            final boolean showCta,
            @NonNull final AffirmColor affirmColor,
            @NonNull final AffirmLogoType affirmLogoType,
            @NonNull SpannablePromoCallback callback
    ) {
        this.promoId = promoId;
        this.pageType = pageType;
        this.dollarAmount = dollarAmount;
        this.showCta = showCta;
        this.affirmColor = affirmColor;
        this.affirmLogoType = affirmLogoType;
        this.callback = callback;
    }

    @Override
    public void create() {
        int centAmount = AffirmUtils.decimalDollarsToIntegerCents(dollarAmount);
        StringBuilder path = new StringBuilder(
                String.format(
                        Locale.getDefault(),
                        PROMO_PATH,
                        AffirmPlugins.get().publicKey(),
                        centAmount,
                        showCta
                )
        );

        if (promoId != null) {
            path.append("&promo_external_id=").append(promoId);
        }

        if (pageType != null) {
            path.append("&page_type=").append(pageType.getType());
        }

        path.append("&logo_color=").append(affirmColor.getColor()).append("&logo_type=").append(affirmLogoType.getType());

        if (promoCall != null) {
            promoCall.cancel();
        }

        promoCall = AffirmPlugins.get().restClient().getCallForRequest(
                new AffirmHttpRequest.Builder()
                        .setUrl(AffirmHttpClient.getProtocol() + AffirmPlugins.get().baseUrl() + path.toString())
                        .setMethod(AffirmHttpRequest.Method.GET)
                        .setTag(TAG_GET_NEW_PROMO)
                        .build()
        );
        promoCall.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                ResponseBody responseBody = response.body();
                Gson gson = AffirmPlugins.get().gson();

                if (response.isSuccessful()) {
                    if (responseBody != null) {
                        handleSuccessResponse(gson.fromJson(responseBody.string(), PromoResponse.class));
                    } else {
                        handleErrorResponse(new APIException("Response was success, but body was null", null));
                    }
                } else {
                    AffirmException affirmException = AffirmHttpClient.createExceptionAndTrackFromResponse(call.request(), response, responseBody);

                    if (affirmException == null) {
                        affirmException = new APIException("Response was not successful", null);
                    }

                    handleErrorResponse(affirmException);
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                AffirmTracker.track(NETWORK_ERROR, ERROR, createTrackingNetworkJsonObj(call.request(), null));
                handleErrorResponse(e);
            }
        });
    }

    @Override
    public void cancel() {
        if (promoCall != null) {
            promoCall.cancel();
            promoCall = null;
        }
    }

    private void handleSuccessResponse(PromoResponse promoResponse) {
        final boolean showPrequal = !promoResponse.promo().promoConfig().promoStyle().equals("fast");
        final String promo = promoResponse.promo().ala();
        final String htmlPromo = promoResponse.promo().htmlAla();
        new Handler(Looper.getMainLooper()).post(() -> callback.onPromoWritten(promo, htmlPromo, showPrequal));
    }

    private void handleErrorResponse(Exception e) {
        AffirmLog.e(e.toString());
        new Handler(Looper.getMainLooper()).post(() -> callback.onFailure(new APIException(e.getMessage(), e)));
    }
}
