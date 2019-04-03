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

    @SerializedName("storeName")
    public abstract String storeName();

    @SerializedName("orderId")
    public abstract String orderId();

    @SerializedName("paymentMethod")
    public abstract String paymentMethod();

    @Nullable
    @SerializedName("coupon")
    public abstract String coupon();

    @Nullable
    @SerializedName("currency")
    public abstract String currency();

    @Nullable
    @SerializedName("discount")
    public abstract Integer discount();

    @Nullable
    @SerializedName("revenue")
    public abstract Integer revenue();

    @Nullable
    @SerializedName("shipping")
    public abstract Integer shipping();

    @Nullable
    @SerializedName("shippingMethod")
    public abstract String shippingMethod();

    @Nullable
    @SerializedName("tax")
    public abstract Integer tax();

    @Nullable
    @SerializedName("total")
    public abstract Integer total();


    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setStoreName(String value);

        public abstract Builder setOrderId(String value);

        public abstract Builder setPaymentMethod(String value);

        public abstract Builder setCoupon(String value);

        public abstract Builder setCurrency(String value);

        public abstract Builder setDiscount(Integer value);

        public abstract Builder setRevenue(Integer value);

        public abstract Builder setShipping(Integer value);

        public abstract Builder setShippingMethod(String value);

        public abstract Builder setTax(Integer value);

        public abstract Builder setTotal(Integer value);

        public abstract AffirmTrackOrder build();
    }
}