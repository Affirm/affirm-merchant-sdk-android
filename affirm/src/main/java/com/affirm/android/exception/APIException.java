package com.affirm.android.exception;

import androidx.annotation.Nullable;

public class APIException extends AffirmException {

    public APIException(@Nullable String message, @Nullable String requestId,
                        @Nullable Integer statusCode, @Nullable AffirmError stripeError,
                        @Nullable Throwable e) {
        super(stripeError, message, requestId, statusCode, e);
    }
}
