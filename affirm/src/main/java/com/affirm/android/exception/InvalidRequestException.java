package com.affirm.android.exception;

import com.affirm.android.model.AffirmError;
import androidx.annotation.Nullable;

import java.util.List;

public class InvalidRequestException extends AffirmException {

    @Nullable
    private final String type;
    @Nullable
    private final String field;
    @Nullable
    private final List<String> fields;

    public InvalidRequestException(@Nullable String message,
                                   @Nullable String type,
                                   @Nullable List<String> fields,
                                   @Nullable String field,
                                   @Nullable String requestId,
                                   @Nullable Integer statusCode,
                                   @Nullable AffirmError affirmError,
                                   @Nullable Throwable e) {
        super(affirmError, message, requestId, statusCode, e);

        this.type = type;
        this.field = field;
        this.fields = fields;
    }

    @Nullable
    public String getType() {
        return type;
    }

    @Nullable
    public String getField() {
        return field;
    }

    @Nullable
    public List<String> getFields() {
        return fields;
    }
}
