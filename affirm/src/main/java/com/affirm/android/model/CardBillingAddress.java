package com.affirm.android.model;

import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class CardBillingAddress implements Parcelable {

    public static CardBillingAddress.Builder builder() {
        return new AutoValue_CardBillingAddress.Builder();
    }

    public static TypeAdapter<CardBillingAddress> typeAdapter(Gson gson) {
        return new AutoValue_CardBillingAddress.GsonTypeAdapter(gson);
    }

    @Nullable
    @SerializedName("line1")
    public abstract String line1();

    @Nullable
    @SerializedName("line2")
    public abstract String line2();

    @Nullable
    @SerializedName("city")
    public abstract String city();

    @Nullable
    @SerializedName("state")
    public abstract String state();

    @Nullable
    @SerializedName("zipcode")
    public abstract String zipcode();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setLine1(String value);

        public abstract Builder setLine2(String value);

        public abstract Builder setCity(String value);

        public abstract Builder setState(String value);

        public abstract Builder setZipcode(String value);

        public abstract CardBillingAddress build();
    }
}
