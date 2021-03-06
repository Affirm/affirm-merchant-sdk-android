package com.affirm.android.model;

import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class VcnReason implements Parcelable {
    public static VcnReason.Builder builder() {
        return new AutoValue_VcnReason.Builder();
    }

    public static TypeAdapter<VcnReason> typeAdapter(Gson gson) {
        return new AutoValue_VcnReason.GsonTypeAdapter(gson);
    }

    @SerializedName("reason")
    public abstract String reason();

    @Nullable
    @SerializedName("checkout_token")
    public abstract String checkoutToken();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract VcnReason.Builder setReason(String value);

        public abstract VcnReason.Builder setCheckoutToken(String value);

        public abstract VcnReason build();
    }
}