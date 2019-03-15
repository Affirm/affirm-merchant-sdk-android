package com.affirm.android.exception;

import androidx.annotation.Nullable;

public class ConnectionException extends AffirmException {

    public ConnectionException(@Nullable String message) {
        this(message, null);
    }

    public ConnectionException(@Nullable String message, @Nullable Throwable e) {
        super(null, message, null, 0, e);
    }
}
