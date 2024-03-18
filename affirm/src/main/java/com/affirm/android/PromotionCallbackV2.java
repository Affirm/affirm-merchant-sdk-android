package com.affirm.android;

import androidx.annotation.NonNull;

import com.affirm.android.exception.AffirmException;

public interface PromotionCallbackV2 {
    void onSuccess(@NonNull Promotion promotion);

    void onFailure(@NonNull AffirmException exception);
}