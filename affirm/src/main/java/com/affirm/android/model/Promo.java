package com.affirm.android.model;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

@AutoValue
public abstract class Promo {
    public abstract String ala();

    @SerializedName("config")
    public abstract PromoConfig promoConfig();

    public static TypeAdapter<Promo> typeAdapter(Gson gson) {
        return new AutoValue_Promo.GsonTypeAdapter(gson);
    }

    public static Builder builder() {
        return new AutoValue_Promo.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setAla(String value);

        public abstract Builder setPromoConfig(PromoConfig value);

        public abstract Promo build();
    }
}
