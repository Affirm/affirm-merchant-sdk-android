package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

@AutoValue
public abstract class Merchant implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Merchant.Builder();
    }

    public static TypeAdapter<Merchant> typeAdapter(Gson gson) {
        return new AutoValue_Merchant.GsonTypeAdapter(gson);
    }

    @SerializedName("public_api_key")
    public abstract String publicApiKey();

    @Nullable
    @SerializedName("user_confirmation_url")
    public abstract String confirmationUrl();

    @Nullable
    @SerializedName("user_cancel_url")
    public abstract String cancelUrl();

    @Nullable
    @SerializedName("name")
    public abstract String name();

    @Nullable
    @SerializedName("use_vcn")
    public abstract Boolean useVcn();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setPublicApiKey(String value);

        public abstract Builder setConfirmationUrl(String value);

        public abstract Builder setCancelUrl(String value);

        public abstract Builder setName(String value);

        public abstract Builder setUseVcn(Boolean value);

        public abstract Merchant build();
    }
}
