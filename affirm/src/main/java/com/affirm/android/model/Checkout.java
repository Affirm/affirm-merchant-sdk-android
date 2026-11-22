package com.affirm.android.model;

import android.os.Parcelable;

import com.affirm.android.AffirmUtils;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AutoValue
public abstract class Checkout implements Parcelable {

    public enum CheckoutMode {
        STANDARD,
        EXPRESS
    }

    public static Builder builder() {
        return new AutoValue_Checkout.Builder().setCheckoutMode(CheckoutMode.STANDARD);
    }

    /**
     * Creates an Express Checkout. Affirm collects the shipping address and calls
     * {@code shippingAndTotalsCallbackUrl} for shipping options, tax and the final total.
     * <p>
     * Express Checkout does not support VCN: starting it with {@code useVCN} or
     * {@code startNewVcnCheckoutFlow} throws {@link IllegalArgumentException}.
     *
     * @param items                        the items being purchased
     * @param orderId                      your unique identifier for the order
     * @param shippingAndTotalsCallbackUrl your Shipping &amp; Totals HTTP endpoint
     * @param metadata                     must contain "subtotal" in integer cents as a string,
     *                                     e.g. "20000"
     * @throws IllegalStateException if {@code items}, {@code orderId},
     *                               {@code shippingAndTotalsCallbackUrl} or
     *                               {@code metadata["subtotal"]} is empty
     */
    public static Checkout createExpressCheckout(
            @NonNull Map<String, Item> items,
            @NonNull String orderId,
            @NonNull String shippingAndTotalsCallbackUrl,
            @NonNull Map<String, String> metadata) {
        return builder()
                .setCheckoutMode(CheckoutMode.EXPRESS)
                .setItems(items)
                .setOrderId(orderId)
                .setShippingAndTotalsCallbackUrl(shippingAndTotalsCallbackUrl)
                .setMetadata(metadata)
                .setTotal(BigDecimal.ZERO)
                .setShippingAmount(BigDecimal.ZERO)
                .setTaxAmount(BigDecimal.ZERO)
                .build();
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
    @SerializedName("metadata")
    public abstract Map<String, String> metadata();

    // A financing program applied on this checkout.
    @Nullable
    @SerializedName("financing_program")
    public abstract String financingProgram();

    @NonNull
    public abstract CheckoutMode checkoutMode();

    // Merchant-hosted Shipping & Totals endpoint. Required for Express Checkout.
    @Nullable
    @SerializedName("merchant_shipping_and_totals_callback_url")
    public abstract String shippingAndTotalsCallbackUrl();

    private void requireExpressFields() {
        final Map<String, String> metadata = metadata();
        final List<String> missing = new ArrayList<>();
        if (isEmpty(orderId())) {
            missing.add("orderId");
        }
        if (isEmpty(shippingAndTotalsCallbackUrl())) {
            missing.add("shippingAndTotalsCallbackUrl");
        }
        if (items().isEmpty()) {
            missing.add("items");
        }
        if (metadata == null || isEmpty(metadata.get("subtotal"))) {
            missing.add("metadata[\"subtotal\"]");
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Express Checkout requires "
                    + Joiner.on(", ").join(missing));
        }
    }

    // Not TextUtils: merchants build checkouts in JVM unit tests, where Android APIs throw.
    private static boolean isEmpty(@Nullable String value) {
        return value == null || value.isEmpty();
    }

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

        public abstract Builder setMetadata(Map<String, String> value);

        public abstract Builder setCurrency(String value);

        public Builder setCurrency(Currency value) {
            return setCurrency(value != null ? value.name() : null);
        }

        public abstract Builder setFinancingProgram(String value);

        public abstract Builder setCheckoutMode(CheckoutMode value);

        abstract CheckoutMode checkoutMode();

        public abstract Builder setShippingAndTotalsCallbackUrl(String value);

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
            // Express Checkout collects the shipping address inside Affirm.
            if (sendShippingAddresses && shipping == null
                    && checkoutMode() != CheckoutMode.EXPRESS) {
                throw new NullPointerException("Null shipping");
            }
            if (billing != null
                    && billing.address() == null
                    && billing.name() == null
                    && billing.phoneNumber() == null
                    && billing.email() == null) {
                setBillingAddress(null);
            }
            Checkout checkout = autoBuild();
            if (checkout.checkoutMode() == CheckoutMode.EXPRESS) {
                checkout.requireExpressFields();
            }
            return checkout;
        }
    }
}
