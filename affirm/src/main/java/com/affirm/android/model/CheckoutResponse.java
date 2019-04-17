package com.affirm.android.model;


import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class CheckoutResponse {
    @SerializedName("redirect_url")
    public abstract String redirectUrl();

    public static TypeAdapter<CheckoutResponse> typeAdapter(Gson gson) {
        return new AutoValue_CheckoutResponse.GsonTypeAdapter(gson);
    }

    public static CheckoutResponse.Builder builder() {
        return new AutoValue_CheckoutResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract CheckoutResponse.Builder setRedirectUrl(String value);

        public abstract CheckoutResponse build();
    }
}
