package com.affirm.android.model;

import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class Billing implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Billing.Builder();
    }

    public static TypeAdapter<Billing> typeAdapter(Gson gson) {
        return new AutoValue_Billing.GsonTypeAdapter(gson);
    }

    @Nullable
    public abstract Address address();

    @Nullable
    public abstract Name name();

    @Nullable
    @SerializedName("phone_number")
    public abstract String phoneNumber();

    @Nullable
    public abstract String email();


    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setAddress(Address value);

        public abstract Builder setName(Name value);

        public abstract Builder setPhoneNumber(String value);

        public abstract Builder setEmail(String value);

        public abstract Billing build();
    }
}
