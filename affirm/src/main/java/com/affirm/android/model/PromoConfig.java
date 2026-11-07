package com.affirm.android.model;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AutoValue
public abstract class PromoConfig {

    static final String MIGRATE_NATIVE_TO_HTML_RENDER = "android_sdk_migrate_native_to_html_render";

    public static final String FLAG_TRUE = "true";
    public static final String FLAG_FALSE = "false";
    public static final String FLAG_MISSING = "missing";
    public static final String FLAG_NULL = "null";
    public static final String FLAG_MALFORMED = "malformed";

    @SerializedName("promo_prequal_enabled")
    public abstract boolean promoPrequalEnabled();

    @Nullable
    @SerializedName("promo_style")
    public abstract String promoStyle();

    // Object so Gson never throws on a malformed flag; only a JSON boolean true migrates.
    // JsonNull.INSTANCE marks an explicit JSON null, which Gson never produces for an Object.
    @Nullable
    @SerializedName(MIGRATE_NATIVE_TO_HTML_RENDER)
    abstract Object migrateNativeToHtmlRenderValue();

    public boolean migrateNativeToHtmlRender() {
        return Boolean.TRUE.equals(migrateNativeToHtmlRenderValue());
    }

    /** How the flag arrived: one of the {@code FLAG_} values. */
    @NonNull
    public String migrateNativeToHtmlRenderStatus() {
        final Object value = migrateNativeToHtmlRenderValue();
        if (value == null) {
            return FLAG_MISSING;
        }
        if (value == JsonNull.INSTANCE) {
            return FLAG_NULL;
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? FLAG_TRUE : FLAG_FALSE;
        }
        return FLAG_MALFORMED;
    }

    public static TypeAdapter<PromoConfig> typeAdapter(Gson gson) {
        final TypeAdapter<PromoConfig> generated = new AutoValue_PromoConfig.GsonTypeAdapter(gson);
        final TypeAdapter<JsonElement> elements = gson.getAdapter(JsonElement.class);
        // The generated adapter skips JSON nulls, so an explicit null would read as missing.
        return new TypeAdapter<PromoConfig>() {
            @Override
            public void write(JsonWriter out, PromoConfig value) throws IOException {
                generated.write(out, value);
            }

            @Override
            public PromoConfig read(JsonReader in) throws IOException {
                final JsonElement tree = elements.read(in);
                final PromoConfig config = generated.fromJsonTree(tree);
                if (config != null && tree.isJsonObject()) {
                    final JsonObject json = tree.getAsJsonObject();
                    if (json.has(MIGRATE_NATIVE_TO_HTML_RENDER)
                            && json.get(MIGRATE_NATIVE_TO_HTML_RENDER).isJsonNull()) {
                        return config.toBuilder()
                                .setMigrateNativeToHtmlRenderValue(JsonNull.INSTANCE)
                                .build();
                    }
                }
                return config;
            }
        };
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
