package com.affirm.android;

import android.text.SpannableString;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.AffirmException;

public interface PromotionCallback {
    void onSuccess(@Nullable SpannableString spannableString);

    void onFailure(@NonNull AffirmException exception);
}
