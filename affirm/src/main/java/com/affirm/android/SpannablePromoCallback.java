package com.affirm.android;

import com.affirm.android.exception.AffirmException;

interface SpannablePromoCallback {
    void onPromoWritten(final String promo, final boolean showPrequal);

    void onFailure(AffirmException exception);
}
