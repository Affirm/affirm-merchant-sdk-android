package com.affirm.android.exception;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class AffirmException extends Exception {

    protected static final long serialVersionUID = 1L;

    @Nullable
    private final String mRequestId;
    @Nullable
    private final Integer mStatusCode;
    @Nullable
    private final AffirmError mStripeError;

    public AffirmException(@Nullable String message, @Nullable String requestId,
                           @Nullable Integer statusCode) {
        this(null, message, requestId, statusCode);
    }

    public AffirmException(@Nullable AffirmError stripeError, @Nullable String message,
                           @Nullable String requestId, @Nullable Integer statusCode) {
        this(stripeError, message, requestId, statusCode, null);
    }

    public AffirmException(@Nullable String message, @Nullable String requestId,
                           @Nullable Integer statusCode, @Nullable Throwable e) {
        this(null, message, requestId, statusCode, e);
    }

    public AffirmException(@Nullable AffirmError stripeError, @Nullable String message,
                           @Nullable String requestId, @Nullable Integer statusCode,
                           @Nullable Throwable e) {
        super(message, e);
        mStripeError = stripeError;
        mStatusCode = statusCode;
        mRequestId = requestId;
    }

    @Nullable
    public String getRequestId() {
        return mRequestId;
    }

    @Nullable
    public Integer getStatusCode() {
        return mStatusCode;
    }

    @Nullable
    public AffirmError getStripeError() {
        return mStripeError;
    }

    @NonNull
    @Override
    public String toString() {
        final String reqIdStr;
        if (mRequestId != null) {
            reqIdStr = "; request-id: " + mRequestId;
        } else {
            reqIdStr = "";
        }
        return super.toString() + reqIdStr;
    }
}

