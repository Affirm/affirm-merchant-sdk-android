package com.affirm.android;

import android.content.Context;
import android.os.AsyncTask;

import com.affirm.android.exception.APIException;
import com.affirm.android.exception.InvalidRequestException;
import com.affirm.android.exception.PermissionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class CheckoutRequest extends Request {

    public enum CheckoutType {
        REGULAR, VCN
    }

    @NonNull
    private Context context;
    @NonNull
    private Checkout checkout;
    @Nullable
    private CheckoutCallback callback;
    @NonNull
    private CheckoutType checkoutType;

    CheckoutRequest(@NonNull Context context, @NonNull Checkout checkout,
                    @Nullable CheckoutCallback callback,
                    @NonNull CheckoutType checkoutType) {
        this.context = context;
        this.checkout = checkout;
        this.callback = callback;
        this.checkoutType = checkoutType;
    }

    @Override
    void create() {
        requestCreate.create();
    }

    @Override
    void cancel() {
        requestCreate.cancel();
    }

    @Override
    AsyncTask createTask() {
        return new CheckoutTask(context, checkout, callback);
    }

    @Override
    void cancelTask() {
        switch (this.checkoutType) {
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

    private static class CheckoutTask extends
            AsyncTask<Void, Void, ResponseWrapper<CheckoutResponse>> {
        @NonNull
        private final Checkout checkout;
        @NonNull
        private final WeakReference<CheckoutCallback> mCallbackRef;

        @NonNull
        private final WeakReference<Context> mContextRef;

        CheckoutTask(@NonNull Context context,
                     @NonNull final Checkout checkout,
                     @Nullable final CheckoutCallback callback) {
            this.mContextRef = new WeakReference<>(context);
            this.checkout = checkout;
            this.mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected ResponseWrapper<CheckoutResponse> doInBackground(Void... params) {
            if (mContextRef.get() != null && mContextRef.get() instanceof CheckoutCommonActivity) {
                try {
                    CheckoutCommonActivity checkoutBaseActivity =
                            (CheckoutCommonActivity) mContextRef.get();
                    CheckoutResponse checkoutResponse = checkoutBaseActivity.executeTask(checkout);
                    return new ResponseWrapper<>(checkoutResponse);
                } catch (IOException e) {
                    return new ResponseWrapper<>(e);
                } catch (APIException e) {
                    return new ResponseWrapper<>(e);
                } catch (PermissionException e) {
                    return new ResponseWrapper<>(e);
                } catch (InvalidRequestException e) {
                    return new ResponseWrapper<>(e);
                }
            } else {
                return new ResponseWrapper<>(new Exception());
            }
        }

        @Override
        protected void onPostExecute(ResponseWrapper<CheckoutResponse> result) {
            final CheckoutCallback checkoutCallback = mCallbackRef.get();
            if (checkoutCallback != null && !isRequestCancelled) {
                if (result.source != null) {
                    checkoutCallback.onSuccess(result.source);
                } else if (result.error != null) {
                    checkoutCallback.onError(result.error);
                }
            }
        }
    }
}
