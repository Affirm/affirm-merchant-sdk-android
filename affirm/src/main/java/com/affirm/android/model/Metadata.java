package com.affirm.android.model;

import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class Metadata implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Metadata.Builder();
    }

    public static TypeAdapter<Metadata> typeAdapter(Gson gson) {
        return new AutoValue_Metadata.GsonTypeAdapter(gson);
    }

    @Nullable
    @SerializedName("shipping_type")
    public abstract String shippingType();

    @Nullable
    @SerializedName("entity_name")
    public abstract String entityName();

    @Nullable
    @SerializedName("webhook_session_id")
    public abstract String webhookSessionId();

    @Nullable
    @SerializedName("mode")
    public abstract String mode();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setShippingType(String value);

        public abstract Builder setEntityName(String value);

        public abstract Builder setWebhookSessionId(String value);

        public abstract Builder setMode(String value);

        public abstract Metadata build();
    }
}
