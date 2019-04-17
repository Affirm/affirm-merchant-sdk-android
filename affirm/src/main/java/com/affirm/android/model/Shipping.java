package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class Shipping implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Shipping.Builder();
    }

    public static TypeAdapter<Shipping> typeAdapter(Gson gson) {
        return new AutoValue_Shipping.GsonTypeAdapter(gson);
    }

    public abstract Address address();

    public abstract Name name();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setAddress(Address value);

        public abstract Builder setName(Name value);

        public abstract Shipping build();
    }
}
