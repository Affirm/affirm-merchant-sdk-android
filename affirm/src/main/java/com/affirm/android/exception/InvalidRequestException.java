package com.affirm.android.exception;


import com.affirm.android.model.AffirmError;

import androidx.annotation.Nullable;

public class InvalidRequestException extends AffirmException {

    @Nullable
    private final String mType;
    @Nullable
    private final String mField;

    public InvalidRequestException(@Nullable String message, @Nullable String type,
                                   @Nullable String field, @Nullable String requestId,
                                   @Nullable Integer statusCode, @Nullable AffirmError affirmError,
                                   @Nullable Throwable e) {
        super(affirmError, message, requestId, statusCode, e);
        mType = type;
        mField = field;
    }

    @Nullable
    public String getType() {
        return mType;
    }

    @Nullable
    public String getField() {
        return mField;
    }
}
