package com.affirm.android;

import android.content.Context;
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

class CheckoutRequest extends AffirmRequest {

    public enum CheckoutType {
        REGULAR, VCN
    }

    @NonNull
    private Context mContext;
    @NonNull
    private Checkout mCheckout;
    @Nullable
    private InnerCheckoutCallback mCallback;
    @NonNull
    private CheckoutType mCheckoutType;

    CheckoutRequest(@NonNull Context context, @NonNull Checkout checkout,
                    @Nullable InnerCheckoutCallback callback,
                    @NonNull CheckoutType checkoutType) {
        mContext = context;
        mCheckout = checkout;
        mCallback = callback;
        mCheckoutType = checkoutType;
    }

    @Override
    void cancel() {
        super.cancel();
        switch (mCheckoutType) {
            case REGULAR:
                AffirmApiHandler.cancelCheckoutCall();
                break;
            case VCN:
                AffirmApiHandler.cancelVcnCheckoutCall();
                break;
            default:
                break;
        }
    }

    @Override
    AsyncTask createTask() {
        return new CheckoutTask(mContext, mCheckout, mCallback);
    }

    private static class CheckoutTask extends
            AsyncTask<Void, Void, AffirmResponseWrapper<CheckoutResponse>> {
        @NonNull
        private final Checkout mCheckout;
        @NonNull
        private final WeakReference<InnerCheckoutCallback> mCallbackRef;

        @NonNull
        private final WeakReference<Context> mContextRef;

        CheckoutTask(@NonNull Context context,
                     @NonNull final Checkout checkout,
                     @Nullable final InnerCheckoutCallback callback) {
            mContextRef = new WeakReference<>(context);
            mCheckout = checkout;
            mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected AffirmResponseWrapper<CheckoutResponse> doInBackground(Void... params) {
            try {
                CheckoutBaseActivity checkoutBaseActivity =
                        (CheckoutBaseActivity) mContextRef.get();
                CheckoutResponse checkoutResponse = checkoutBaseActivity.executeTask(mCheckout);
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
            if (checkoutCallback != null && !isRequestCancelled) {
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
