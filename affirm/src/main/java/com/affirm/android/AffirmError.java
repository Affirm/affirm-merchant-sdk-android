package com.affirm.android;

import com.affirm.android.model.ErrorResponse;

class AffirmError extends Exception {
    private final ErrorResponse errorResponse;

    public AffirmError(ErrorResponse errorResponse) {
        this.errorResponse = errorResponse;
    }

    @Override
    public String getMessage() {
        return errorResponse.message();
    }

    @Override
    public String toString() {
        return errorResponse.toString();
    }
}
