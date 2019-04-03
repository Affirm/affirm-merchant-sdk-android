package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

import java.util.List;

@AutoValue
public abstract class AffirmTrackObject implements Parcelable {

    public abstract AffirmTrackOrder affirmTrackOrder();

    public abstract List<AffirmTrackProduct> affirmTrackProducts();

    public static AffirmTrackObject.Builder builder() {
        return new AutoValue_AffirmTrackObject.Builder();
    }

    public static TypeAdapter<AffirmTrackObject> typeAdapter(Gson gson) {
        return new AutoValue_AffirmTrackObject.GsonTypeAdapter(gson);
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder setAffirmTrackOrder(AffirmTrackOrder value);

        public abstract Builder setAffirmTrackProducts(List<AffirmTrackProduct> value);

        public abstract AffirmTrackObject build();
    }
}
