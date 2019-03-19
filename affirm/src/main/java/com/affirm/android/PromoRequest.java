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
    private final String mPromoId;
    private final float mDollarAmount;
    private final boolean mShowCta;
    @NonNull
    private SpannablePromoCallback mCallback;

    PromoRequest(@Nullable final String promoId, final float dollarAmount,
                 final boolean showCta, @NonNull SpannablePromoCallback callback) {
        mPromoId = promoId;
        mDollarAmount = dollarAmount;
        mShowCta = showCta;
        mCallback = callback;
    }

    @Override
    void cancel() {
        super.cancel();
        AffirmApiHandler.cancelNewPromoCall();
    }

    @Override
    AsyncTask createTask() {
        return new PromoTask(mPromoId, mDollarAmount, mShowCta, mCallback);
    }

    private static class PromoTask extends
            AsyncTask<Void, Void, ResponseWrapper<PromoResponse>> {
        @Nullable
        private final String mPromoId;
        private final float mDollarAmount;
        private final boolean mShowCta;
        @NonNull
        private final WeakReference<SpannablePromoCallback> mCallbackRef;

        PromoTask(@Nullable String promoId,
                  float dollarAmount,
                  boolean showCta,
                  @NonNull SpannablePromoCallback callback) {
            mPromoId = promoId;
            mDollarAmount = dollarAmount;
            mShowCta = showCta;
            mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected ResponseWrapper<PromoResponse> doInBackground(Void... params) {
            try {
                PromoResponse promoResponse =
                        AffirmApiHandler.getNewPromo(mPromoId, mDollarAmount, mShowCta);
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
