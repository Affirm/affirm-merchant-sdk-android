package com.affirm.android.model;

import android.os.Parcelable;

import com.affirm.android.AffirmUtils;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AutoValue
public abstract class Checkout implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Checkout.Builder();
    }

    public static TypeAdapter<Checkout> typeAdapter(Gson gson) {
        return new AutoValue_Checkout.GsonTypeAdapter(gson);
    }

    public abstract Map<String, Item> items();

    @Nullable
    public abstract Map<String, Discount> discounts();

    public abstract Shipping shipping();

    @Nullable
    public abstract Shipping billing();

    @SerializedName("shipping_amount")
    public abstract Integer shippingAmount();

    @SerializedName("tax_amount")
    public abstract Integer taxAmount();

    public abstract Integer total();

    @Nullable
    public abstract Map<String, String> metadata();

    @AutoValue.Builder
    public abstract static class Builder {
        private Float mCheckoutTotal;
        private Float mTaxAmount;
        private Float mShippingAmount;

        public abstract Builder setItems(Map<String, Item> value);

        public abstract Builder setDiscounts(Map<String, Discount> value);

        public abstract Builder setShipping(Shipping value);

        public abstract Builder setBilling(Shipping value);

        abstract Builder setShippingAmount(Integer value);

        abstract Builder setTaxAmount(Integer value);

        abstract Builder setTotal(Integer value);

        public abstract Builder setMetadata(Map<String, String> value);

        abstract Checkout autoBuild();

        public Builder setTotal(@NonNull Float value) {
            mCheckoutTotal = value;
            return this;
        }

        public Builder setShippingAmount(@NonNull Float value) {
            mShippingAmount = value;
            return this;
        }

        public Builder setTaxAmount(@NonNull Float value) {
            mTaxAmount = value;
            return this;
        }

        public Checkout build() {
            setTotal(AffirmUtils.decimalDollarsToIntegerCents(mCheckoutTotal));
            setShippingAmount(AffirmUtils.decimalDollarsToIntegerCents(mShippingAmount));
            setTaxAmount(AffirmUtils.decimalDollarsToIntegerCents(mTaxAmount));
            return autoBuild();
        }
    }
}
