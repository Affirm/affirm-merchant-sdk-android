package com.affirm.android;

import com.affirm.android.model.CheckoutResponse;

interface CheckoutCallback {

    void onError(Exception exception);

    void onSuccess(CheckoutResponse response);
}

