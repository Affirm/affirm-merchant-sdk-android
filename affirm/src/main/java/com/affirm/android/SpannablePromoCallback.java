package com.affirm.android;

import android.text.SpannableString;

public interface SpannablePromoCallback {
    void onPromoWritten(final String promo, final boolean showPrequal);

    void onFailure(Throwable throwable);
}
