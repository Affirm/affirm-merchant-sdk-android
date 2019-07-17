package com.affirm.android;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.APIException;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.PromoResponse;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

class PromoRequest extends AffirmRequest<Void> {

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
    Void createTask() {
        Handler handler =  new Handler(Looper.getMainLooper());

        AffirmApiHandler.fetchPromo(
                promoId,
                pageType,
                dollarAmount,
                showCta,
                affirmColor.getColor(),
                affirmLogoType.getType(),
                new AffirmApiHandler.ApiCallback() {
                    @Override
                    public void onSuccess(@NotNull PromoResponse response) {
                        final boolean showPrequal =
                                !response.promo().promoConfig().promoStyle().equals("fast");
                        final String promo = response.promo().ala();
                        final String htmlPromo = response.promo().htmlAla();

                       handler.post(() -> callback.onPromoWritten(promo, htmlPromo, showPrequal));
                    }

                    @Override
                    public void onError(@NonNull IOException e) {
                        AffirmLog.e(e.toString());
                        handler.post(() ->
                                callback.onFailure(new APIException(e.getMessage(), null, null, null, null))
                        );
                    }
                }
        );

        return null;
    }

    @Override
    void cancelTask() {
        AffirmApiHandler.cancelNewPromoCall();
    }
}
