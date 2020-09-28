package com.affirm.android.model;


import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class CardCancelResponse {
    @SerializedName("message")
    public abstract String message();

    @SerializedName("code")
    public abstract String code();

    @SerializedName("checkout_token")
    public abstract String checkoutToken();

    public static TypeAdapter<CardCancelResponse> typeAdapter(Gson gson) {
        return new AutoValue_CardCancelResponse.GsonTypeAdapter(gson);
    }

    public static CardCancelResponse.Builder builder() {
        return new AutoValue_CardCancelResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract CardCancelResponse.Builder setMessage(String value);

        public abstract CardCancelResponse.Builder setCode(String code);

        public abstract CardCancelResponse.Builder setCheckoutToken(String value);

        public abstract CardCancelResponse build();
    }
}
