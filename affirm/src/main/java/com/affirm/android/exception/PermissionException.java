package com.affirm.android.exception;


import com.affirm.android.model.AffirmError;

import androidx.annotation.Nullable;

public class PermissionException extends AffirmException {

    public PermissionException(@Nullable String message, @Nullable String requestId,
                               @Nullable Integer statusCode, @Nullable AffirmError stripeError) {
        super(stripeError, message, requestId, statusCode);
    }
}
