package com.affirm.android.exception;

import androidx.annotation.Nullable;

public class APIConnectionException extends AffirmException {

    public APIConnectionException(@Nullable String message) {
        this(message, null);
    }

    public APIConnectionException(@Nullable String message, @Nullable Throwable e) {
        super(null, message, null, 0, e);
    }

}
