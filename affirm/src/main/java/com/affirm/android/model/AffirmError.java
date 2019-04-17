package com.affirm.android.model;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class AffirmError {
    public abstract String message();

    @SerializedName("status_code")
    public abstract Integer status();

    public abstract String field();

    public abstract String code();

    public abstract String type();

    public static TypeAdapter<AffirmError> typeAdapter(Gson gson) {
        return new AutoValue_AffirmError.GsonTypeAdapter(gson);
    }
}
