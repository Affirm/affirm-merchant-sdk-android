package com.affirm.android;

import android.os.AsyncTask;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.PromoPageType;
import com.affirm.android.model.PromoResponse;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class PromoRequest extends AffirmRequest {

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

    PromoRequest(@Nullable final String promoId,
                 @Nullable final PromoPageType pageType,
                 final float dollarAmount,
                 final boolean showCta,
                 @NonNull final AffirmColor affirmColor,
                 @NonNull final AffirmLogoType affirmLogoType,
                 @NonNull SpannablePromoCallback callback) {
        this.promoId = promoId;
        this.pageType = pageType;
        this.dollarAmount = dollarAmount;
        this.showCta = showCta;
        this.affirmColor = affirmColor;
        this.affirmLogoType = affirmLogoType;
        this.callback = callback;
    }

    @Override
    void cancel() {
        super.cancel();
        AffirmApiHandler.cancelNewPromoCall();
    }

    @Override
    AsyncTask createTask() {
        return new PromoTask(promoId, pageType, dollarAmount, showCta,
                affirmColor, affirmLogoType, callback);
    }

    @Override
    void cancelTask() {
        if (task != null) {
            ((PromoTask) task).cancelTask();
        }
    }

    private static class PromoTask extends
            AsyncTask<Void, Void, AffirmResponseWrapper<PromoResponse>> {
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
        private final WeakReference<SpannablePromoCallback> mCallbackRef;

        void cancelTask() {
            mCallbackRef.clear();
        }

        PromoTask(@Nullable String promoId,
                  @Nullable PromoPageType pageType,
                  float dollarAmount,
                  boolean showCta,
                  @NonNull AffirmColor affirmColor,
                  @NonNull AffirmLogoType affirmLogoType,
                  @NonNull SpannablePromoCallback callback) {
            this.promoId = promoId;
            this.pageType = pageType;
            this.dollarAmount = dollarAmount;
            this.showCta = showCta;
            this.affirmColor = affirmColor;
            this.affirmLogoType = affirmLogoType;
            mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected AffirmResponseWrapper<PromoResponse> doInBackground(Void... params) {
            try {
                PromoResponse promoResponse =
                        AffirmApiHandler.getNewPromo(
                                promoId,
                                pageType,
                                dollarAmount,
                                showCta,
                                affirmColor.getColor(),
                                affirmLogoType.getType());
                return new AffirmResponseWrapper<>(promoResponse);
            } catch (ConnectionException e) {
                return new AffirmResponseWrapper<>(e);
            } catch (APIException e) {
                return new AffirmResponseWrapper<>(e);
            } catch (PermissionException e) {
                return new AffirmResponseWrapper<>(e);
            } catch (InvalidRequestException e) {
                return new AffirmResponseWrapper<>(e);
            }
        }

        @Override
        protected void onPostExecute(@NonNull AffirmResponseWrapper<PromoResponse> result) {
            final SpannablePromoCallback callback = mCallbackRef.get();
            if (callback != null) {
                if (result.source != null) {
                    final boolean showPrequal =
                            !result.source.promo().promoConfig().promoStyle().equals("fast");
                    final String promo = result.source.promo().ala();
                    final String htmlPromo = result.source.promo().htmlAla();
                    callback.onPromoWritten(promo, htmlPromo, showPrequal);
                } else if (result.error != null) {
                    AffirmLog.e(result.error.toString());
                    callback.onFailure(result.error);
                }
            }
        }
    }
}
