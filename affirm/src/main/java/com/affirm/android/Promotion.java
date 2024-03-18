package com.affirm.android;

import android.text.SpannableString;

import androidx.annotation.NonNull;

public class Promotion {
    @NonNull
    private SpannableString spannableString;
    @NonNull
    private String description;
    private boolean showPrequal;

    public Promotion(@NonNull SpannableString spannableString,
                     @NonNull String description,
                     boolean showPrequal) {
        this.spannableString = spannableString;
        this.description = description;
        this.showPrequal = showPrequal;
    }

    @NonNull
    public SpannableString getSpannableString() {
        return spannableString;
    }

    public void setSpannableString(@NonNull SpannableString spannableString) {
        this.spannableString = spannableString;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    public boolean isShowPrequal() {
        return showPrequal;
    }

    public void setShowPrequal(boolean showPrequal) {
        this.showPrequal = showPrequal;
    }
}