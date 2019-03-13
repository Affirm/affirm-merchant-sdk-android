package com.affirm.android;

interface SpannablePromoCallback {
    void onPromoWritten(final String promo, final boolean showPrequal);

    void onFailure(Throwable throwable);
}
