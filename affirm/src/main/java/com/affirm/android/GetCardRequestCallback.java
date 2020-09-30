package com.affirm.android;

import androidx.annotation.NonNull;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.CardDetails;

interface GetCardRequestCallback {

    void onError(@NonNull AffirmException exception);

    void onGetCardSuccess(@NonNull CardDetails response);
}

