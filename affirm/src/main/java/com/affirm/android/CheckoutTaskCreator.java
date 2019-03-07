package com.affirm.android;

import android.content.Context;

import com.affirm.android.model.Checkout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

interface CheckoutTaskCreator {

    void create(
            @NonNull final Context context,
            @NonNull final Checkout checkout,
            @Nullable final CheckoutBaseActivity.CheckoutCallback callback);

    void cancel();
}
