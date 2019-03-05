package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import androidx.annotation.Nullable;

@AutoValue
public abstract class Address implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Address.Builder();
    }

    public static TypeAdapter<Address> typeAdapter(Gson gson) {
        return new AutoValue_Address.GsonTypeAdapter(gson);
    }

    public abstract String line1();

    @Nullable
    public abstract String line2();

    public abstract String city();

    public abstract String state();

    public abstract String zipcode();

    public abstract String country();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setLine1(String value);

        public abstract Builder setLine2(String value);

        public abstract Builder setCity(String value);

        public abstract Builder setState(String value);

        public abstract Builder setZipcode(String value);

        public abstract Builder setCountry(String value);

        public abstract Address build();
    }
}
