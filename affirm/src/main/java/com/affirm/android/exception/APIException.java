package com.affirm.android.exception;

import com.affirm.android.model.AffirmError;

import androidx.annotation.Nullable;

public class APIException extends AffirmException {

    public APIException(@Nullable String message, @Nullable String requestId,
                        @Nullable Integer statusCode, @Nullable AffirmError affirmError,
                        @Nullable Throwable e) {
        super(affirmError, message, requestId, statusCode, e);
    }
}
