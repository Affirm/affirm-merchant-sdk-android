package com.affirm.android;

import com.affirm.android.model.PromoResponse;

import java.io.IOException;

/**
 * Affirm
 * Created by gerry on 05/03/2019
 * Copyright © 2017 Affirm. All rights reserved.
 */
class AffirmPromoRequest {

    private boolean isRequestCancelled = false;

    AffirmPromoRequest() {
    }

    CancellableRequest getNewPromo(final String promoId, final float dollarAmount,
                                   final boolean showCta,
                                   final SpannablePromoCallback promoCallback) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    PromoResponse response = AffirmApiHandler.getNewPromo(promoId, dollarAmount,
                            showCta);
                    if (promoCallback != null && !isRequestCancelled) {
                        boolean showPrequal =
                                !response.promo().promoConfig().promoStyle().equals("fast");
                        String promo = response.promo().ala();
                        promoCallback.onPromoWritten(promo, showPrequal);
                    }
                } catch (IOException e) {
                    if (promoCallback != null && !isRequestCancelled) {
                        promoCallback.onFailure(e);
                    }
                }
            }
        };
        new Thread(runnable).start();
        return new CancellableRequest() {
            @Override
            public void cancelRequest() {
                isRequestCancelled = true;
                AffirmApiHandler.cancelNewPromoCall();
            }
        };
    }
}
