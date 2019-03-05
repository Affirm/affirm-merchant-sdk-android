package com.affirm.android.model;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class ErrorResponse {
    public abstract String message();

    @SerializedName("status_code")
    public abstract Integer status();

    public static TypeAdapter<ErrorResponse> typeAdapter(Gson gson) {
        return new AutoValue_ErrorResponse.GsonTypeAdapter(gson);
    }
}
