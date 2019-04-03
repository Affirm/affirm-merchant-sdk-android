package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

@AutoValue
public abstract class AffirmTrackProduct implements Parcelable {
    public static Builder builder() {
        return new AutoValue_AffirmTrackProduct.Builder();
    }

    public static TypeAdapter<AffirmTrackProduct> typeAdapter(Gson gson) {
        return new AutoValue_AffirmTrackProduct.GsonTypeAdapter(gson);
    }

    @SerializedName("productId")
    public abstract String productId();

    @Nullable
    @SerializedName("brand")
    public abstract String brand();

    @Nullable
    @SerializedName("category")
    public abstract String category();

    @Nullable
    @SerializedName("coupon")
    public abstract String coupon();

    @Nullable
    @SerializedName("name")
    public abstract String name();

    @Nullable
    @SerializedName("price")
    public abstract Integer price();

    @Nullable
    @SerializedName("quantity")
    public abstract Integer quantity();

    @Nullable
    @SerializedName("variant")
    public abstract String variant();


    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setProductId(String value);

        public abstract Builder setBrand(String value);

        public abstract Builder setCategory(String value);

        public abstract Builder setCoupon(String value);

        public abstract Builder setName(String value);

        public abstract Builder setPrice(Integer value);

        public abstract Builder setQuantity(Integer value);

        public abstract Builder setVariant(String value);

        public abstract AffirmTrackProduct build();
    }
}