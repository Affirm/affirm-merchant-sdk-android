package com.affirm.android;

import androidx.annotation.NonNull;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.CardCancelResponse;
import com.affirm.android.model.CardDetails;

interface CardRequestCallback {

    void onError(@NonNull AffirmException exception);

    void onCardCancelSuccess(@NonNull CardCancelResponse response, @NonNull CardRequestType type);

    void onCardFetchSuccess(@NonNull CardDetails response, @NonNull CardRequestType type);
}

