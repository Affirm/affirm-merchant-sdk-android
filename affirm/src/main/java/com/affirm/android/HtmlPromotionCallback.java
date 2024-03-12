package com.affirm.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.affirm.android.exception.AffirmException;

public interface HtmlPromotionCallback {
    void onSuccess(@Nullable String htmlPromo,
                   @Nullable String promoDescription,
                   boolean showPrequal);

    void onFailure(@NonNull AffirmException exception);
}