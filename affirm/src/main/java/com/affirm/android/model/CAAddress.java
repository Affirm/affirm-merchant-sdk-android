package com.affirm.android.model;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class CAAddress extends Address {

    public static CAAddress.Builder builder() {
        return new AutoValue_CAAddress.Builder();
    }

    public static TypeAdapter<CAAddress> typeAdapter(Gson gson) {
        return new AutoValue_CAAddress.GsonTypeAdapter(gson);
    }

    // Valid street address, verified by public address service APIs.
    @SerializedName("street1")
    public abstract String street1();

    // Apartment, suite, floor, etc.
    @Nullable
    @SerializedName("street2")
    public abstract String street2();

    // City name, verified by public address service APIs.
    @SerializedName("city")
    public abstract String city();

    // 2-letter ISO code for the country province, verified by public address service APIs.
    @SerializedName("region1_code")
    public abstract String region1Code();

    // Must match other provided address information, verified by public address service APIs.
    @SerializedName("postal_code")
    public abstract String postalCode();

    // If provided, must be 'CA'. Affirm is only available to U.S. residents.
    @Nullable
    @SerializedName("country_code")
    public abstract String countryCode();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setStreet1(String value);

        public abstract Builder setStreet2(String value);

        public abstract Builder setCity(String value);

        public abstract Builder setRegion1Code(String value);

        public abstract Builder setPostalCode(String value);

        public abstract Builder setCountryCode(String value);

        public abstract CAAddress build();
    }
}
