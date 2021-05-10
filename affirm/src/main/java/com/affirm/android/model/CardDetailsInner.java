package com.affirm.android.model;

import java.util.Date;

public class CardDetailsInner {

    private CardDetails cardDetails;

    private Date expirationDate;

    public CardDetails getCardDetails() {
        return cardDetails;
    }

    public Date getExpirationDate() {
        return (Date) expirationDate.clone();
    }

    public CardDetailsInner(CardDetails cardDetails, Date expirationDate) {
        this.cardDetails = cardDetails;
        this.expirationDate = (Date) expirationDate.clone();
    }
}
