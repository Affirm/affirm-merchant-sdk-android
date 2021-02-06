package com.affirm.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@AutoValue
public abstract class AffirmError {
    @Nullable
    public abstract String message();

    @SerializedName("status_code")
    @Nullable
    public abstract Integer status();

    @Nullable
    public abstract List<String> fields();

    @Nullable
    public abstract String field();

    @Nullable
    public abstract String code();

    @Nullable
    public abstract String type();

    public static TypeAdapter<AffirmError> typeAdapter(Gson gson) {
        return new AutoValue_AffirmError.GsonTypeAdapter(gson);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Affirm error message: ");
        if (message() != null) {
            builder.append(message());
        }
        if (status() != null) {
            builder.append(", status_code: ").append(status());
        }
        if (field() != null) {
            builder.append(", field: ").append(field());
        }
        if (fields() != null) {
            builder.append(", fields: ").append(fields());
        }
        if (code() != null) {
            builder.append(", code: ").append(code());
        }
        if (type() != null) {
            builder.append(", type: ").append(type());
        }
        return builder.toString();
    }
}
