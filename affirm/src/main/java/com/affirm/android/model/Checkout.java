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

    // Your internal unique identifier representing the order. Maximum 500 characters. Required
    @Nullable
    @SerializedName("orderId")
    public abstract String orderId();

    // A list of item objects.
    public abstract Map<String, Item> items();

    // A hash of coupon codes to discount objects. If discounts are passed, they must have a name
    // and positive integer dollar amount.
    @Nullable
    public abstract Map<String, Discount> discounts();

    // Customer contact information.
    public abstract Shipping shipping();

    // Customer contact information.
    @Nullable
    public abstract Shipping billing();

    // The total shipping amount; Defaults to 0.
    @SerializedName("shipping_amount")
    public abstract Integer shippingAmount();

    // The total tax amount computed after all discounts have been applied; Defaults to 0.
    @SerializedName("tax_amount")
    public abstract Integer taxAmount();

    // The total amount of the checkout. This determines the total amount charged to the user.
    //Note: We only use this value for the loan total; we do not calculate the total from the
    // checkout object line items.
    public abstract Integer total();

    // A hash of keys to values for any metadata to be passed into checkout and stored.
    // 'entity_name' is a protected key, and will show up in your settlement reporting.
    @Nullable
    public abstract Map<String, String> metadata();

    @AutoValue.Builder
    public abstract static class Builder {
        private Float mCheckoutTotal;
        private Float mTaxAmount;
        private Float mShippingAmount;

        public abstract Builder setOrderId(String value);

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
