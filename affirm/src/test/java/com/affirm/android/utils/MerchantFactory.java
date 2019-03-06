package com.affirm.android.utils;

import com.affirm.android.AffirmWebViewClient;
import com.affirm.android.model.Merchant;

public class MerchantFactory {

    public static Merchant create() {
        return Merchant.builder()
                .setPublicApiKey("sdf")
                .setConfirmationUrl(AffirmWebViewClient.AFFIRM_CONFIRMATION_URL)
                .setCancelUrl(AffirmWebViewClient.AFFIRM_CANCELLATION_URL)
                .build();
    }
}
