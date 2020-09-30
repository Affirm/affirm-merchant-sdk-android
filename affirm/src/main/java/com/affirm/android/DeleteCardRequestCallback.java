package com.affirm.android;

import androidx.annotation.NonNull;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.CardCancelResponse;

interface DeleteCardRequestCallback {

    void onError(@NonNull AffirmException exception);

    void onCancelCardSuccess(@NonNull CardCancelResponse response);
}

