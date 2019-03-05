package com.affirm.android.model;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class PromoResponse {
    public abstract Promo promo();

    public static TypeAdapter<PromoResponse> typeAdapter(Gson gson) {
        return new AutoValue_PromoResponse.GsonTypeAdapter(gson);
    }

    public static Builder builder() {
        return new AutoValue_PromoResponse.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setPromo(Promo value);

        public abstract PromoResponse build();
    }
}
