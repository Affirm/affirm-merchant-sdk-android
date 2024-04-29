package com.affirm.android;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.Item;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.PromoResponse;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;

import static com.affirm.android.AffirmConstants.LOGO_PLACEHOLDER;
import static com.affirm.android.AffirmConstants.PROMO_AMOUNT;
import static com.affirm.android.AffirmConstants.PROMO_EXTERNAL_ID;
import static com.affirm.android.AffirmConstants.PROMO_FIELD;
import static com.affirm.android.AffirmConstants.PROMO_FIELD_VALUE;
import static com.affirm.android.AffirmConstants.PROMO_IS_SDK;
import static com.affirm.android.AffirmConstants.PROMO_ITEMS;
import static com.affirm.android.AffirmConstants.PROMO_LOCALE;
import static com.affirm.android.AffirmConstants.PROMO_LOGO_COLOR;
import static com.affirm.android.AffirmConstants.PROMO_LOGO_TYPE;
import static com.affirm.android.AffirmConstants.PROMO_PAGE_TYPE;
import static com.affirm.android.AffirmConstants.PROMO_PATH;
import static com.affirm.android.AffirmConstants.PROMO_SHOW_CTA;

class PromoRequest implements AffirmRequest {

    @Nullable
    private final OkHttpClient okHttpClient;
    @Nullable
    private final String promoId;
    private final BigDecimal dollarAmount;
    private final boolean showCta;
    @NonNull
    private final AffirmColor affirmColor;
    @NonNull
    private final AffirmLogoType affirmLogoType;
    @Nullable
    private final PromoPageType pageType;
    @Nullable
    private final List<Item> items;
    @NonNull
    private final SpannablePromoCallback callback;

    private final boolean isHtmlStyle;

    private Call promoCall;

    PromoRequest(
            @Nullable final String promoId,
            @Nullable final PromoPageType pageType,
            final BigDecimal dollarAmount,
            final boolean showCta,
            @NonNull final AffirmColor affirmColor,
            @NonNull final AffirmLogoType affirmLogoType,
            boolean isHtmlStyle,
            @Nullable List<Item> items,
            @NonNull SpannablePromoCallback callback
    ) {
        this(null, promoId, pageType, dollarAmount, showCta,
                affirmColor, affirmLogoType, isHtmlStyle, items, callback);
    }

    @VisibleForTesting
    PromoRequest(
            @Nullable OkHttpClient okHttpClient,
            @Nullable final String promoId,
            @Nullable final PromoPageType pageType,
            final BigDecimal dollarAmount,
            final boolean showCta,
            @NonNull final AffirmColor affirmColor,
            @NonNull final AffirmLogoType affirmLogoType,
            boolean isHtmlStyle,
            @Nullable List<Item> items,
            @NonNull SpannablePromoCallback callback
    ) {
        this.okHttpClient = okHttpClient;
        this.promoId = promoId;
        this.pageType = pageType;
        this.dollarAmount = dollarAmount;
        this.showCta = showCta;
        this.affirmColor = affirmColor;
        this.affirmLogoType = affirmLogoType;
        this.isHtmlStyle = isHtmlStyle;
        this.items = items;
        this.callback = callback;
    }

    @Override
    public void create() {
        if (dollarAmount.compareTo(AffirmConstants.maxPrice) > 0) {
            handleErrorResponse(new IllegalArgumentException(
                    "Affirm: data-amount is higher than the maximum ($17500)."));
            return;
        }

        if (promoCall != null) {
            promoCall.cancel();
        }

        promoCall = AffirmClient.send(okHttpClient, new AffirmPromoRequest(),
                new AffirmClient.AffirmListener<PromoResponse>() {
                    @Override
                    public void onSuccess(PromoResponse response) {
                        handleSuccessResponse(response);
                    }

                    @Override
                    public void onFailure(AffirmException exception) {
                        callback.onFailure(exception);
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
        final boolean showPrequal = !promoResponse.promo()
                .promoConfig()
                .promoStyle()
                .equals("fast");

        final String promo = promoResponse.promo().ala();
        final String htmlPromo = promoResponse.promo().htmlAla();

        final String promoMessage = isHtmlStyle ? htmlPromo : promo;
        final String promoDescription = promo.replace(LOGO_PLACEHOLDER, "affirm");
        if (TextUtils.isEmpty(promoMessage)) {
            handleErrorResponse(new Exception("Promo message is null or empty!"));
        } else {
            callback.onPromoWritten(promoMessage, promoDescription, showPrequal);
        }
    }

    private void handleErrorResponse(Exception e) {
        callback.onFailure(new APIException(e.getMessage(), e));
    }

    class AffirmPromoRequest implements AffirmClient.AffirmApiRequest {

        @NotNull
        @Override
        public String url() {
            int centAmount = AffirmUtils.decimalDollarsToIntegerCents(dollarAmount);
            Uri uri = Uri.parse(String.format(
                    AffirmHttpClient.getProtocol()
                            + AffirmPlugins.get().promoUrl()
                            + PROMO_PATH,
                    AffirmPlugins.get().publicKey()
            ));
            Uri.Builder builder = uri.buildUpon();
            builder.appendQueryParameter(PROMO_IS_SDK, String.valueOf(true));
            builder.appendQueryParameter(PROMO_FIELD, PROMO_FIELD_VALUE);
            builder.appendQueryParameter(PROMO_AMOUNT, String.valueOf(centAmount));
            builder.appendQueryParameter(PROMO_SHOW_CTA, String.valueOf(showCta));
            if (promoId != null) {
                builder.appendQueryParameter(PROMO_EXTERNAL_ID, promoId);
            }
            if (pageType != null) {
                builder.appendQueryParameter(PROMO_PAGE_TYPE, pageType.getType());
            }
            builder.appendQueryParameter(PROMO_LOGO_COLOR, affirmColor.getColor());
            builder.appendQueryParameter(PROMO_LOGO_TYPE, affirmLogoType.getType());
            if (items != null) {
                builder.appendQueryParameter(PROMO_ITEMS,
                        Uri.encode(AffirmPlugins.get().gson().toJson(items))
                );
            }
            builder.appendQueryParameter(PROMO_LOCALE, AffirmPlugins.get().locale());
            return builder.build().toString();
        }

        @NotNull
        @Override
        public AffirmHttpRequest.Method method() {
            return AffirmHttpRequest.Method.GET;
        }

        @Nullable
        @Override
        public JsonObject body() {
            return null;
        }

        @Nullable
        @Override
        public Map<String, String> headers() {
            return null;
        }
    }
}
