package com.affirm.android.model;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

@AutoValue
public abstract class PromoConfig {

    @SerializedName("promo_prequal_enabled")
    public abstract boolean promoPrequalEnabled();

    @Nullable
    @SerializedName("promo_style")
    public abstract String promoStyle();

    // Object so Gson never throws on a malformed flag; only a JSON boolean true migrates.
    @Nullable
    @SerializedName("android_sdk_migrate_native_to_html_render")
    abstract Object migrateNativeToHtmlRenderValue();

    public boolean migrateNativeToHtmlRender() {
        return Boolean.TRUE.equals(migrateNativeToHtmlRenderValue());
    }

    public static TypeAdapter<PromoConfig> typeAdapter(Gson gson) {
        return new AutoValue_PromoConfig.GsonTypeAdapter(gson);
    }

    public static Builder builder() {
        return new AutoValue_PromoConfig.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setPromoPrequalEnabled(boolean value);

        public abstract Builder setPromoStyle(String value);

        public abstract Builder setMigrateNativeToHtmlRenderValue(Object value);

        public abstract PromoConfig build();
    }
}
