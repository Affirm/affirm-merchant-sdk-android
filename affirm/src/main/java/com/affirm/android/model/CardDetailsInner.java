package com.affirm.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class CardDetailsInner implements Parcelable {

    public static final Creator<CardDetailsInner> CREATOR = new Creator<CardDetailsInner>() {
        @Override
        public CardDetailsInner createFromParcel(Parcel in) {
            return new CardDetailsInner(
                    (CardDetails) in.readParcelable(CardDetails.class.getClassLoader()),
                    (Date) in.readSerializable()
            );
        }

        @Override
        public CardDetailsInner[] newArray(int size) {
            return new CardDetailsInner[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(cardDetails, flags);
        dest.writeSerializable(expirationDate);
    }
}
