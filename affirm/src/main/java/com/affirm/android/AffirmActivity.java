package com.affirm.android;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.affirm.android.model.CardDetails;
import com.affirm.android.model.VcnReason;

import static com.affirm.android.Affirm.RESULT_ERROR;
import static com.affirm.android.AffirmConstants.CHECKOUT_ERROR;
import static com.affirm.android.AffirmConstants.CHECKOUT_TOKEN;
import static com.affirm.android.AffirmConstants.CREDIT_DETAILS;
import static com.affirm.android.AffirmConstants.PREQUAL_ERROR;
import static com.affirm.android.AffirmConstants.VCN_REASON;

abstract class AffirmActivity extends AppCompatActivity {

    protected void finishWithError(@Nullable String error) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_ERROR, error);
        setResult(RESULT_ERROR, intent);
        finish();
    }

    protected void finishWithToken(String token) {
        final Intent intent = new Intent();
        intent.putExtra(CHECKOUT_TOKEN, token);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    protected void finishWithCancellation() {
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    protected void finishWithCardDetail(@NonNull CardDetails cardDetails) {
        final Intent intent = new Intent();
        intent.putExtra(CREDIT_DETAILS, cardDetails);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    protected void finishWithVcnReason(@NonNull VcnReason vcnReason) {
        final Intent intent = new Intent();
        intent.putExtra(VCN_REASON, vcnReason);
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
    }

    protected void finishWithPrequalError(@Nullable String error) {
        final Intent intent = new Intent();
        intent.putExtra(PREQUAL_ERROR, error);
        setResult(RESULT_ERROR, intent);
        finish();
    }
}
