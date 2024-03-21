package com.affirm.android;

import androidx.annotation.NonNull;

import com.affirm.android.exception.AffirmException;

public interface HtmlPromotionCallbackV2 {
    void onSuccess(HtmlPromotion promotion);

    void onFailure(@NonNull AffirmException exception);
}
