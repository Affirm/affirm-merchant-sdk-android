package com.affirm.android.model;

import android.os.Parcelable;

import com.affirm.android.AffirmUtils;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;
import java.util.List;

@AutoValue
public abstract class Item implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Item.Builder();
    }

    public static TypeAdapter<Item> typeAdapter(Gson gson) {
        return new AutoValue_Item.GsonTypeAdapter(gson);
    }

    // The display name of the product.
    @SerializedName("display_name")
    public abstract String displayName();

    // The product SKU.
    public abstract String sku();

    // The item price expressed as integer USD cents ("$100" = 10000).
    @SerializedName("unit_price")
    public abstract Integer unitPrice();

    // The item quantity expressed as an integer.
    public abstract Integer qty();

    // The item's product description page URL.
    @Nullable
    @SerializedName("item_url")
    public abstract String url();

    // The item's product image URL.
    @Nullable
    @SerializedName("item_image_url")
    public abstract String imageUrl();

    // An array of lists that indicate the various categories that apply to this product, and
    // the hierarchy of those category definitions. Each list in the array contains one or more
    // comma-separated strings, with the first string being the highest-level (widest) category.
    @Nullable
    @SerializedName("categories")
    public abstract List<List<String>> categories();

    @AutoValue.Builder
    public abstract static class Builder {
        private BigDecimal mUnitPrice;

        public abstract Builder setDisplayName(String value);

        public abstract Builder setSku(String value);

        abstract Builder setUnitPrice(Integer value);

        public abstract Builder setQty(Integer value);

        public abstract Builder setUrl(String value);

        public abstract Builder setImageUrl(String value);

        public abstract Builder setCategories(@Nullable List<List<String>> categories);

        abstract Item autoBuild();

        public Builder setUnitPrice(@NonNull BigDecimal value) {
            mUnitPrice = value;
            return this;
        }

        public Item build() {
            setUnitPrice(AffirmUtils.decimalDollarsToIntegerCents(mUnitPrice));
            return autoBuild();
        }
    }
}
