package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.Nullable;

@AutoValue
public abstract class Merchant implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Merchant.Builder();
    }

    public static TypeAdapter<Merchant> typeAdapter(Gson gson) {
        return new AutoValue_Merchant.GsonTypeAdapter(gson);
    }

    @SerializedName("public_api_key")
    public abstract String publicApiKey();

    // URL that the customer is sent to if they successfully complete the Affirm checkout flow.
    // A checkout_token will be sent to this URL in the POST request, and that checkout_token
    // should be used to authorize the charge before the user is redirected to the order
    // confirmation page.
    // Analytics tags are other query string parameters can be persisted here as well.
    @Nullable
    @SerializedName("user_confirmation_url")
    public abstract String confirmationUrl();

    // URL that the customer is sent to if the customer exits the Affirm checkout flow.
    // This is the same if the user voluntarily cancels or closes the window before completion,
    // or if the user is denied. You should setup the cancel_url to be the checkout payment page,
    // and you can also append analytics tags to the URL to help you identify who you may want to
    // reach out to about alternative payment methods or reapplying.
    // Analytics tags are other query string parameters can be persisted here as well.
    @Nullable
    @SerializedName("user_cancel_url")
    public abstract String cancelUrl();

    // If you have multiple sites operating under a single Affirm account, you can override the
    // external company/brand name that the customer sees. This affects all references to your
    // company name in the Affirm UI.
    @Nullable
    @SerializedName("name")
    public abstract String name();

    @Nullable
    @SerializedName("use_vcn")
    public abstract Boolean useVcn();

    @Nullable
    @SerializedName("caas")
    public abstract String caas();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setPublicApiKey(String value);

        public abstract Builder setConfirmationUrl(String value);

        public abstract Builder setCancelUrl(String value);

        public abstract Builder setName(String value);

        public abstract Builder setUseVcn(Boolean value);

        public abstract Builder setCaas(String value);

        public abstract Merchant build();
    }
}
