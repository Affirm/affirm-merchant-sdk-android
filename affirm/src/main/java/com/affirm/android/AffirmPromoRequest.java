package com.affirm.android;

import android.os.AsyncTask;

import com.affirm.android.model.PromoResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;

/**
 * Affirm
 * Created by gerry on 05/03/2019
 * Copyright © 2017 Affirm. All rights reserved.
 */
class AffirmPromoRequest {

    private static boolean isRequestCancelled = false;

    private PromoTask promoTask;

    AffirmPromoRequest() {
    }

    CancellableRequest getNewPromo(final String promoId, final float dollarAmount,
                                   final boolean showCta,
                                   final PromoCallback promoCallback) {
        return new CancellableRequest() {
            @Override
            public void cancelRequest() {
                if (promoTask != null && !promoTask.isCancelled()) {
                    promoTask.cancel(true);
                    promoTask = null;
                }
                isRequestCancelled = true;
                AffirmApiHandler.cancelNewPromoCall();
            }

            @Override
            public void executeRequest() {
                promoTask = new PromoTask(promoId, dollarAmount, showCta, promoCallback);
                promoTask.execute();
            }
        };
    }

    private static class PromoResponseWrapper {

        PromoResponse source;

        Exception error;

        PromoResponseWrapper(PromoResponse source) {
            this.source = source;
        }

        PromoResponseWrapper(Exception error) {
            this.error = error;
        }
    }

    private static class PromoTask extends AsyncTask<Void, Void, PromoResponseWrapper> {
        @NonNull
        private final String promoId;
        private final float dollarAmount;
        private final boolean showCta;
        @NonNull
        private final WeakReference<PromoCallback> mCallbackRef;

        PromoTask(@NonNull String promoId,
                  float dollarAmount,
                  boolean showCta,
                  @NonNull PromoCallback callback) {
            this.promoId = promoId;
            this.dollarAmount = dollarAmount;
            this.showCta = showCta;
            this.mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected PromoResponseWrapper doInBackground(Void... params) {
            try {
                return new PromoResponseWrapper(AffirmApiHandler.getNewPromo(promoId, dollarAmount, showCta));
            } catch (IOException e) {
                return new PromoResponseWrapper(e);
            }
        }

        @Override
        protected void onPostExecute(PromoResponseWrapper result) {
            final PromoCallback callback = mCallbackRef.get();
            if (callback != null && !isRequestCancelled) {
                if (result.source != null) {
                    boolean showPrequal = !result.source.promo().promoConfig().promoStyle().equals("fast");
                    String promo = result.source.promo().ala();
                    callback.onPromoWritten(promo, showPrequal);
                } else if (result.error != null) {
                    callback.onFailure(result.error);
                }
            }
        }
    }
}
