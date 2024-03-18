package com.affirm.android;

import com.affirm.android.exception.AffirmException;

import androidx.annotation.NonNull;

interface SpannablePromoCallback {
    void onPromoWritten(@NonNull final String promoMessage,
                        @NonNull final String promoDescription,
                        final boolean showPrequal);

    void onFailure(@NonNull AffirmException exception);
}
