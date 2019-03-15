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

    private AsyncTask checkoutTask;

    void create(@NonNull Context context, @NonNull Checkout checkout,
                @Nullable CheckoutCallback callback) {
        checkoutCreator.create(context, checkout, callback);
    }

    void cancel(@NonNull CheckoutType type) {
        checkoutCreator.cancel(type);
    }

    interface CheckoutCreator {

        void create(
                @NonNull final Context context,
                @NonNull final Checkout checkout,
                @Nullable final CheckoutCallback callback);

        void cancel(@NonNull CheckoutType type);
    }

    private final CheckoutCreator checkoutCreator = new CheckoutCreator() {
        @Override
        public void create(@NonNull Context context, @NonNull Checkout checkout,
                           @Nullable CheckoutCallback callback) {
            isRequestCancelled = false;
            checkoutTask = new CheckoutTask(context, checkout, callback);
            executeTask(AsyncTask.THREAD_POOL_EXECUTOR, checkoutTask);
        }

        @Override
        public void cancel(@NonNull CheckoutType type) {
            if (checkoutTask != null && !checkoutTask.isCancelled()) {
                checkoutTask.cancel(true);
                checkoutTask = null;
            }

            isRequestCancelled = true;
            switch (type) {
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
    };

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
