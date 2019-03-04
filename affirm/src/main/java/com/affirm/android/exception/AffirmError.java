package com.affirm.android.exception;

import androidx.annotation.Nullable;

public class AffirmError {

    @Nullable
    public final String type;
    @Nullable
    public final String message;

    @Nullable
    public final String code;
    @Nullable
    public final String param;

    @Nullable
    public final String declineCode;

    @Nullable
    public final String charge;

    AffirmError(@Nullable String type, @Nullable String message, @Nullable String code,
                @Nullable String param, @Nullable String declineCode, @Nullable String charge) {
        this.type = type;
        this.message = message;
        this.code = code;
        this.param = param;
        this.declineCode = declineCode;
        this.charge = charge;
    }
}
