package com.affirm.android.utils;


import com.affirm.android.model.CardDetails;

public class CardDetailsFactory {
    public static CardDetails create() {
        return CardDetails.builder()
                .setCardholderName("John Smith")
                .setCheckoutToken("1234-1234")
                .setCvv("333")
                .setExpiration("1022")
                .setNumber("4444444444444444")
                .build();
    }
}
