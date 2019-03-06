package com.affirm.android;

interface PromoCallback {
    void onPromoWritten(final String promo, final boolean showPrequal);

    void onFailure(Throwable throwable);
}
