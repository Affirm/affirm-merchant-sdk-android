package com.affirm.android.exception;

import com.affirm.android.model.AffirmError;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public abstract class AffirmException extends Exception {

    protected static final long serialVersionUID = 1L;

    @Nullable
    private final String requestId;
    @Nullable
    private final Integer statusCode;
    @Nullable
    private final AffirmError affirmError;

    public AffirmException(@Nullable String message, @Nullable String requestId,
                           @Nullable Integer statusCode) {
        this(null, message, requestId, statusCode);
    }

    public AffirmException(@Nullable AffirmError affirmError, @Nullable String message,
                           @Nullable String requestId, @Nullable Integer statusCode) {
        this(affirmError, message, requestId, statusCode, null);
    }

    public AffirmException(@Nullable String message, @Nullable String requestId,
                           @Nullable Integer statusCode, @Nullable Throwable e) {
        this(null, message, requestId, statusCode, e);
    }

    public AffirmException(@Nullable AffirmError affirmError, @Nullable String message,
                           @Nullable String requestId, @Nullable Integer statusCode,
                           @Nullable Throwable e) {
        super(message, e);
        this.affirmError = affirmError;
        this.statusCode = statusCode;
        this.requestId = requestId;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    @Nullable
    public Integer getStatusCode() {
        return statusCode;
    }

    @Nullable
    public AffirmError getStripeError() {
        return affirmError;
    }

    @NonNull
    @Override
    public String toString() {
        final String reqIdStr;
        if (requestId != null) {
            reqIdStr = ", request-id: " + requestId;
        } else {
            reqIdStr = "";
        }
        return super.toString() + reqIdStr;
    }
}

