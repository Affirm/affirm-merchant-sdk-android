package com.affirm.android.model;

import android.os.Parcelable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;

@AutoValue
public abstract class Name implements Parcelable {
    public static Builder builder() {
        return new AutoValue_Name.Builder();
    }

    public static TypeAdapter<Name> typeAdapter(Gson gson) {
        return new AutoValue_Name.GsonTypeAdapter(gson);
    }

    // Required if 'first' and 'last' keys are missing. String must contain at two words.
    public abstract String full();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setFull(String value);

        public abstract Name build();
    }
}
