package com.affirm.android.model;

import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class Address implements Parcelable {

    public static Address.Builder builder() {
        return new AutoValue_Address.Builder();
    }

    public static TypeAdapter<Address> typeAdapter(Gson gson) {
        return new AutoValue_Address.GsonTypeAdapter(gson);
    }

    // Valid U.S. street address, verified by public address service APIs.
    public abstract String street1();

    // Apartment, suite, floor, etc.
    @Nullable
    public abstract String street2();

    // City name, verified by public address service APIs.
    public abstract String city();

    // 2-letter ISO code or full name, verified by public address service APIs.
    @SerializedName("region1_code")
    public abstract String region1Code();

    // Must match other provided address information, verified by public address service APIs.
    @SerializedName("postal_code")
    public abstract String postalCode();

    // If provided, must be 'US' or 'USA' (3-letter ISO code).
    // Affirm is only available to U.S. residents.
    public abstract String country();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setStreet1(String value);

        public abstract Builder setStreet2(String value);

        public abstract Builder setCity(String value);

        public abstract Builder setRegion1Code(String value);

        public abstract Builder setPostalCode(String value);

        public abstract Builder setCountry(String value);

        public abstract Address build();
    }
}
