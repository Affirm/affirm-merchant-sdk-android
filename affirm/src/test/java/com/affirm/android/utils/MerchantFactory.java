package com.affirm.android.utils;

import com.affirm.android.model.Merchant;

import static com.affirm.android.Constants.AFFIRM_CANCELLATION_URL;
import static com.affirm.android.Constants.AFFIRM_CONFIRMATION_URL;


public class MerchantFactory {

    public static Merchant create() {
        return Merchant.builder()
                .setPublicApiKey("sdf")
                .setConfirmationUrl(AFFIRM_CONFIRMATION_URL)
                .setCancelUrl(AFFIRM_CANCELLATION_URL)
                .build();
    }
}
