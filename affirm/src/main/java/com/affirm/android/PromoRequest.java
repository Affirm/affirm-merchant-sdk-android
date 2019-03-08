package com.affirm.android;

import android.os.AsyncTask;

import com.affirm.android.model.PromoResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;

class PromoRequest {

    private static boolean isRequestCancelled = false;

    private PromoTask promoTask;

    PromoRequest() {
    }

    CancellableRequest getNewPromo(final String promoId, final float dollarAmount,
                                   final boolean showCta,
                                   final PromoCallback promoCallback) {
        return new CancellableRequest() {
            @Override
            public void cancel() {
                if (promoTask != null && !promoTask.isCancelled()) {
                    promoTask.cancel(true);
                    promoTask = null;
                }
                isRequestCancelled = true;
                AffirmApiHandler.cancelNewPromoCall();
            }

            @Override
            public void execute() {
                isRequestCancelled = false;
                promoTask = new PromoTask(promoId, dollarAmount, showCta, promoCallback);
                promoTask.execute();
            }
        };
    }

    private static class PromoTask extends AsyncTask<Void, Void, ResponseWrapper<PromoResponse>> {
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
        protected ResponseWrapper<PromoResponse> doInBackground(Void... params) {
            try {
                PromoResponse promoResponse = AffirmApiHandler.getNewPromo(promoId, dollarAmount,
                    showCta);
                return new ResponseWrapper<>(promoResponse);
            } catch (IOException e) {
                return new ResponseWrapper<>(e);
            }
        }

        @Override
        protected void onPostExecute(ResponseWrapper<PromoResponse> result) {
            final PromoCallback callback = mCallbackRef.get();
            if (callback != null && !isRequestCancelled) {
                if (result.source != null) {
                    boolean showPrequal =
                        !result.source.promo().promoConfig().promoStyle().equals("fast");
                    String promo = result.source.promo().ala();
                    callback.onPromoWritten(promo, showPrequal);
                } else if (result.error != null) {
                    callback.onFailure(result.error);
                }
            }
        }
    }
}
