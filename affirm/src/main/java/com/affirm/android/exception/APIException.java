package com.affirm.android.exception;

import androidx.annotation.Nullable;

import com.affirm.android.model.AffirmError;

public class APIException extends AffirmException {

    public APIException(@Nullable String message, @Nullable String requestId,
                        @Nullable Integer statusCode, @Nullable AffirmError affirmError,
                        @Nullable Throwable e) {
        super(affirmError, message, requestId, statusCode, e);
    }

    public APIException(@Nullable String message, @Nullable Throwable e) {
        super(null, message, null, null, e);
    }
}
