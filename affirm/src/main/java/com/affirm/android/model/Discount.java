package com.affirm.android.model;

import android.os.Parcelable;

import com.affirm.android.AffirmUtils;
import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;

@AutoValue
public abstract class Discount implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Discount.Builder();
    }

    public static TypeAdapter<Discount> typeAdapter(Gson gson) {
        return new AutoValue_Discount.GsonTypeAdapter(gson);
    }

    @SerializedName("discount_display_name")
    public abstract String displayName();

    @SerializedName("discount_amount")
    public abstract Integer amount();

    @AutoValue.Builder
    public abstract static class Builder {
        private Float mAmount;

        public abstract Builder setDisplayName(String value);

        abstract Builder setAmount(Integer value);

        abstract Discount autoBuild();

        public Builder setAmount(@NonNull Float value) {
            mAmount = value;
            return this;
        }

        public Discount build() {
            setAmount(AffirmUtils.decimalDollarsToIntegerCents(mAmount));
            return autoBuild();
        }
    }
}
