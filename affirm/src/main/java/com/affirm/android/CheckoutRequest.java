package com.affirm.android;

import android.os.AsyncTask;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.ConnectionException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class CheckoutRequest extends AffirmRequest<AsyncTask> {

    @NonNull
    private final Checkout checkout;
    private final boolean useVCN;
    @Nullable
    private final InnerCheckoutCallback callback;

    CheckoutRequest(@NonNull Checkout checkout,
                    @Nullable InnerCheckoutCallback callback,
                    boolean useVCN) {
        this.checkout = checkout;
        this.callback = callback;
        this.useVCN = useVCN;
    }

    @Override
    void cancel() {
        super.cancel();
        if (useVCN) {
            AffirmApiHandler.cancelVcnCheckoutCall();
        } else {
            AffirmApiHandler.cancelCheckoutCall();
        }
    }

    @Override
    AsyncTask createTask() {
        return new CheckoutTask(checkout, useVCN, callback);
    }

    @Override
    void cancelTask() {
        if (task != null) {
            ((CheckoutTask) task).cancelTask();
        }
    }

    private static class CheckoutTask extends
            AsyncTask<Void, Void, AffirmResponseWrapper<CheckoutResponse>> {
        @NonNull
        private final Checkout mCheckout;
        private final boolean mUseVcn;
        @NonNull
        private final WeakReference<InnerCheckoutCallback> mCallbackRef;

        CheckoutTask(@NonNull final Checkout checkout,
                     boolean useVCN,
                     @Nullable final InnerCheckoutCallback callback) {
            mCheckout = checkout;
            mUseVcn = useVCN;
            mCallbackRef = new WeakReference<>(callback);
        }

        void cancelTask() {
            mCallbackRef.clear();
        }

        @Override
        protected AffirmResponseWrapper<CheckoutResponse> doInBackground(Void... params) {
            try {
                CheckoutResponse checkoutResponse;
                if (mUseVcn) {
                    checkoutResponse = AffirmApiHandler.executeVcnCheckout(mCheckout);
                } else {
                    checkoutResponse = AffirmApiHandler.executeCheckout(mCheckout);
                }
                return new AffirmResponseWrapper<>(checkoutResponse);
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
        protected void onPostExecute(@NonNull AffirmResponseWrapper<CheckoutResponse> result) {
            final InnerCheckoutCallback checkoutCallback = mCallbackRef.get();
            if (checkoutCallback != null) {
                if (result.source != null) {
                    checkoutCallback.onSuccess(result.source);
                } else if (result.error != null) {
                    AffirmLog.e(result.error.toString());
                    checkoutCallback.onError(result.error);
                }
            }
        }
    }
}
