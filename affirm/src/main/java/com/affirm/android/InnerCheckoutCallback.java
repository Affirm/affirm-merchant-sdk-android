package com.affirm.android;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.CheckoutResponse;

import androidx.annotation.NonNull;

interface InnerCheckoutCallback {

    void onError(@NonNull AffirmException exception);

    void onSuccess(@NonNull CheckoutResponse response);
}

