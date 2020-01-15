package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

@AutoValue
public abstract class AffirmTrackOrder implements Parcelable {
    public static Builder builder() {
        return new AutoValue_AffirmTrackOrder.Builder();
    }

    public static TypeAdapter<AffirmTrackOrder> typeAdapter(Gson gson) {
        return new AutoValue_AffirmTrackOrder.GsonTypeAdapter(gson);
    }

    // Your store name. Maximum 500 characters. Required
    @SerializedName("storeName")
    public abstract String storeName();

    // Your internal unique identifier representing the order. Maximum 500 characters. Required
    @SerializedName("orderId")
    public abstract String orderId();

    // The payment method chosen by the customer (e.g., Visa). Maximum 150 characters. Required
    @SerializedName("paymentMethod")
    public abstract String paymentMethod();

    // Your internal unique identifier representing the checkout if it’s distinct from the order ID.
    // If they are the same, only orderID is needed. Maximum 500 characters. Optional
    @Nullable
    @SerializedName("checkoutId")
    public abstract String checkoutId();

    // The coupon code applied to the order (e.g., SUMMER2018). Maximum 500 characters. Optional
    @Nullable
    @SerializedName("coupon")
    public abstract String coupon();

    // USD Optional
    @Nullable
    @SerializedName("currency")
    public abstract Currency currency();

    // The total discount applied to the order, stated in USD cents (e.g., $100 = 10000). Optional
    @Nullable
    @SerializedName("discount")
    public abstract Integer discount();

    // The net revenue amount of the order excluding shipping, total tax and discounts,
    // stated in USD cents (e.g., $100 = 10000). Optional
    @Nullable
    @SerializedName("revenue")
    public abstract Integer revenue();

    // The shipping cost associated with the order. Optional
    @Nullable
    @SerializedName("shipping")
    public abstract Integer shipping();

    // The shipping method chosen by the customer (e.g., Fedex). Maximum 150 characters. Optional
    @Nullable
    @SerializedName("shippingMethod")
    public abstract String shippingMethod();

    // The total tax amount associated with the order, stated in USD cents (e.g., $100 = 10000).
    // Optional
    @Nullable
    @SerializedName("tax")
    public abstract Integer tax();

    // The total amount of the transaction including tax and shipping, stated in USD cents
    // (e.g., $100 = 10000). If not sent, the total amount will be calculated using the product
    // quantity and price fields of each product object passed. Optional
    @Nullable
    @SerializedName("total")
    public abstract Integer total();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setStoreName(String value);

        public abstract Builder setOrderId(String value);

        public abstract Builder setPaymentMethod(String value);

        public abstract Builder setCheckoutId(String value);

        public abstract Builder setCoupon(String value);

        public abstract Builder setCurrency(Currency value);

        public abstract Builder setDiscount(Integer value);

        public abstract Builder setRevenue(Integer value);

        public abstract Builder setShipping(Integer value);

        public abstract Builder setShippingMethod(String value);

        public abstract Builder setTax(Integer value);

        public abstract Builder setTotal(Integer value);

        public abstract AffirmTrackOrder build();
    }
}