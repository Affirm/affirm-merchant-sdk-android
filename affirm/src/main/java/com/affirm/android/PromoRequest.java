package com.affirm.android;

import android.os.AsyncTask;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.PromoResponse;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class PromoRequest extends Request {

    @Nullable
    private final String promoId;
    private final float dollarAmount;
    private final boolean showCta;
    @NonNull
    private SpannablePromoCallback callback;

    PromoRequest(@Nullable final String promoId, final float dollarAmount,
                 final boolean showCta, @NonNull SpannablePromoCallback callback) {
        this.promoId = promoId;
        this.dollarAmount = dollarAmount;
        this.showCta = showCta;
        this.callback = callback;
    }

    @Override
    void cancel() {
        super.cancel();
        AffirmApiHandler.cancelNewPromoCall();
    }

    @Override
    AsyncTask createTask() {
        return new PromoTask(promoId, dollarAmount, showCta, callback);
    }

    private static class PromoTask extends
            AsyncTask<Void, Void, ResponseWrapper<PromoResponse>> {
        @Nullable
        private final String promoId;
        private final float dollarAmount;
        private final boolean showCta;
        @NonNull
        private final WeakReference<SpannablePromoCallback> mCallbackRef;

        PromoTask(@Nullable String promoId,
                  float dollarAmount,
                  boolean showCta,
                  @NonNull SpannablePromoCallback callback) {
            this.promoId = promoId;
            this.dollarAmount = dollarAmount;
            this.showCta = showCta;
            this.mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected ResponseWrapper<PromoResponse> doInBackground(Void... params) {
            try {
                PromoResponse promoResponse =
                        AffirmApiHandler.getNewPromo(promoId, dollarAmount, showCta);
                return new ResponseWrapper<>(promoResponse);
            } catch (ConnectionException e) {
                return new ResponseWrapper<>(e);
            } catch (APIException e) {
                return new ResponseWrapper<>(e);
            } catch (PermissionException e) {
                return new ResponseWrapper<>(e);
            } catch (InvalidRequestException e) {
                return new ResponseWrapper<>(e);
            }
        }

        @Override
        protected void onPostExecute(ResponseWrapper<PromoResponse> result) {
            final SpannablePromoCallback callback = mCallbackRef.get();
            if (callback != null && !isRequestCancelled) {
                if (result.source != null) {
                    final boolean showPrequal =
                            !result.source.promo().promoConfig().promoStyle().equals("fast");
                    final String promo = result.source.promo().ala();
                    callback.onPromoWritten(promo, showPrequal);
                } else if (result.error != null) {
                    AffirmLog.e("Get new promo failed...", result.error);
                    callback.onFailure(result.error);
                }
            }
        }
    }
}
