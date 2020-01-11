package com.affirm.android.model;

import android.os.Parcelable;

import com.affirm.android.AffirmUtils;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
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
    @SerializedName("order_id")
    public abstract String orderId();

    // A list of item objects.
    public abstract Map<String, Item> items();

    // A hash of coupon codes to discount objects. If discounts are passed, they must have a name
    // and positive integer dollar amount.
    @Nullable
    public abstract Map<String, Discount> discounts();

    // Enter "CAD" for the currency type.
    @Nullable
    @SerializedName("currency")
    public abstract String currency();

    // Customer contact information.
    // The entire optional should be required, unless `sendShippingAddresses` to false
    // to make it to optional
    @Nullable
    @SerializedName("shipping")
    public abstract Shipping shippingAddress();

    // Customer contact information. optional
    @Nullable
    @SerializedName("billing")
    public abstract Billing billingAddress();

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
        /**
         * A flag whether to send shipping addresses to Affirm server, default is true.
         */
        private boolean sendShippingAddresses = true;

        private Shipping shipping;

        private Billing billing;

        public abstract Builder setOrderId(String value);

        public abstract Builder setItems(Map<String, Item> value);

        public abstract Builder setDiscounts(Map<String, Discount> value);

        abstract Builder setShippingAddress(Shipping value);

        abstract Builder setBillingAddress(Billing value);

        abstract Builder setShippingAmount(Integer value);

        abstract Builder setTaxAmount(Integer value);

        abstract Builder setTotal(Integer value);

        abstract Builder setCurrency(String value);

        public Builder setCurrency(Currency currency) {
            return setCurrency(currency.getValue());
        }

        public abstract Builder setMetadata(Map<String, String> value);

        abstract Checkout autoBuild();

        public Builder setShipping(Shipping value) {
            this.shipping = value;
            return setShippingAddress(value);
        }

        public Builder setBilling(Billing value) {
            this.billing = value;
            return setBillingAddress(value);
        }

        public Builder setTotal(@NonNull BigDecimal value) {
            return setTotal(AffirmUtils.decimalDollarsToIntegerCents(value));
        }

        public Builder setShippingAmount(@NonNull BigDecimal value) {
            return setShippingAmount(AffirmUtils.decimalDollarsToIntegerCents(value));
        }

        public Builder setTaxAmount(@NonNull BigDecimal value) {
            return setTaxAmount(AffirmUtils.decimalDollarsToIntegerCents(value));
        }

        public Builder setSendShippingAddresses(boolean value) {
            sendShippingAddresses = value;
            return this;
        }

        public Checkout build() {
            if (sendShippingAddresses && shipping == null) {
                throw new NullPointerException("Null shipping");
            }
            if (billing != null
                    && billing.address() == null
                    && billing.name() == null
                    && billing.phoneNumber() == null
                    && billing.email() == null) {
                setBillingAddress(null);
            }
            return autoBuild();
        }
    }
}
