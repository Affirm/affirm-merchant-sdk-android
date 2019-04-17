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

    // Your unique internal identifier representing the product, such as the SKU or an internal
    // database identifier. Maximum 500 characters. Required
    @SerializedName("productId")
    public abstract String productId();

    // The brand of the product (e.g., Affirm). Maximum 500 characters. Optional
    @Nullable
    @SerializedName("brand")
    public abstract String brand();

    // The product category (e.g., apparel). Maximum 500 characters. Optional
    @Nullable
    @SerializedName("category")
    public abstract String category();

    // Any coupon code applied to this particular product. Maximum 500 characters. Optional
    @Nullable
    @SerializedName("coupon")
    public abstract String coupon();

    // The full name of the product (e.g., Affirm T-Shirt). Maximum 500 characters. Optional
    @Nullable
    @SerializedName("name")
    public abstract String name();

    // The price of the purchased product, stated in USD cents (e.g., $100 = 10000). Optional
    @Nullable
    @SerializedName("price")
    public abstract Integer price();

    // The quantity of the purchased product. Optional
    @Nullable
    @SerializedName("quantity")
    public abstract Integer quantity();

    // The variant of the product (e.g. black). Maximum 500 characters. Optional
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