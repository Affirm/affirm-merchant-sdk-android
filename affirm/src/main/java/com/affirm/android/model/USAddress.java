package com.affirm.android.model;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class USAddress extends Address {

    public static USAddress.Builder builder() {
        return new AutoValue_USAddress.Builder();
    }

    public static TypeAdapter<USAddress> typeAdapter(Gson gson) {
        return new AutoValue_USAddress.GsonTypeAdapter(gson);
    }

    // Valid U.S. street address, verified by public address service APIs.
    public abstract String line1();

    // Apartment, suite, floor, etc.
    @Nullable
    public abstract String line2();

    // City name, verified by public address service APIs.
    public abstract String city();

    // 2-letter ISO code or full name, verified by public address service APIs.
    public abstract String state();

    // Must match other provided address information, verified by public address service APIs.
    public abstract String zipcode();

    // If provided, must be 'US' or 'USA' (3-letter ISO code).
    // Affirm is only available to U.S. residents.
    public abstract String country();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setLine1(String value);

        public abstract Builder setLine2(String value);

        public abstract Builder setCity(String value);

        public abstract Builder setState(String value);

        public abstract Builder setZipcode(String value);

        public abstract Builder setCountry(String value);

        public abstract USAddress build();
    }
}
