package com.affirm.android.model;

import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@AutoValue
public abstract class AffirmErrorUi implements Parcelable {
    public static TypeAdapter<AffirmErrorUi> typeAdapter(Gson gson) {
        return new AutoValue_AffirmErrorUi.GsonTypeAdapter(gson);
    }

    @Nullable
    public abstract String main();

    @Nullable
    public abstract String sub();

    @SerializedName("sub_extra")
    @Nullable
    public abstract List<String> subExtra();
}
