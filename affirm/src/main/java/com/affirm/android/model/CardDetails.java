package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class CardDetails implements Parcelable {
    public static CardDetails.Builder builder() {
        return new AutoValue_CardDetails.Builder();
    }

    public static TypeAdapter<CardDetails> typeAdapter(Gson gson) {
        return new AutoValue_CardDetails.GsonTypeAdapter(gson);
    }

    @SerializedName("cardholder_name")
    public abstract String cardholderName();

    @SerializedName("checkout_token")
    public abstract String checkoutToken();

    @SerializedName("cvv")
    public abstract String cvv();

    @SerializedName("expiration")
    public abstract String expiration();

    @SerializedName("number")
    public abstract String number();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract CardDetails.Builder setCardholderName(String value);

        public abstract CardDetails.Builder setCheckoutToken(String value);

        public abstract CardDetails.Builder setCvv(String value);

        public abstract CardDetails.Builder setExpiration(String value);

        public abstract CardDetails.Builder setNumber(String value);

        public abstract CardDetails build();
    }
}
