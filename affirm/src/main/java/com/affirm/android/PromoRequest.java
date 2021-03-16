package com.affirm.android;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.Item;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.PromoResponse;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.PROMO_PATH;

class PromoRequest implements AffirmRequest, AffirmApiRepository.AffirmApiListener<PromoResponse> {

    private final AffirmApiRepository repository;
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
        this.promoId = promoId;
        this.pageType = pageType;
        this.dollarAmount = dollarAmount;
        this.showCta = showCta;
        this.affirmColor = affirmColor;
        this.affirmLogoType = affirmLogoType;
        this.isHtmlStyle = isHtmlStyle;
        this.items = items;
        this.callback = callback;
        this.repository = new AffirmApiRepository();
    }

    @Override
    public void create() {
        if (dollarAmount.compareTo(AffirmConstants.maxPrice) > 0) {
            handleErrorResponse(new IllegalArgumentException(
                    "Affirm: data-amount is higher than the maximum ($17500)."));
            return;
        }

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

        path.append("&logo_color=")
                .append(affirmColor.getColor())
                .append("&logo_type=")
                .append(affirmLogoType.getType());

        if (items != null) {
            path.append("&items=").append(Uri.encode(AffirmPlugins.get().gson().toJson(items)));
        }

        final String url = HTTPS_PROTOCOL + AffirmPlugins.get().basePromoUrl() + path.toString();
        this.repository.promoRequest(url, this);
    }

    @Override
    public void onResponse(@NotNull PromoResponse response) {
        handleSuccessResponse(response);
    }

    @Override
    public void onFailed(@NotNull AffirmException exception) {
        handleErrorResponse(exception);
    }

    @Override
    public void cancel() {
        this.repository.cancelRequest();
    }

    private void handleSuccessResponse(PromoResponse promoResponse) {
        final boolean showPrequal = !promoResponse.promo()
                .promoConfig()
                .promoStyle()
                .equals("fast");

        final String promo = promoResponse.promo().ala();
        final String htmlPromo = promoResponse.promo().htmlAla();

        final String promoMessage = isHtmlStyle ? htmlPromo : promo;
        if (TextUtils.isEmpty(promoMessage)) {
            handleErrorResponse(new Exception("Promo message is null or empty!"));
        } else {
            callback.onPromoWritten(promoMessage, showPrequal);
        }
    }

    private void handleErrorResponse(Exception e) {
        callback.onFailure(new APIException(e.getMessage(), e));
    }
}
