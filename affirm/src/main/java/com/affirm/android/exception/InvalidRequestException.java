package com.affirm.android.exception;


import com.affirm.android.model.AffirmError;

import androidx.annotation.Nullable;

public class InvalidRequestException extends AffirmException {

    @Nullable
    private final String type;
    @Nullable
    private final String field;

    public InvalidRequestException(@Nullable String message, @Nullable String type,
                                   @Nullable String field, @Nullable String requestId,
                                   @Nullable Integer statusCode, @Nullable AffirmError affirmError,
                                   @Nullable Throwable e) {
        super(affirmError, message, requestId, statusCode, e);
        this.type = type;
        this.field = field;
    }

    @Nullable
    public String getType() {
        return type;
    }

    @Nullable
    public String getField() {
        return field;
    }
}
