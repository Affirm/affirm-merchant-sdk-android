package com.affirm.android;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.CardCancelResponse;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.widget.CountDownTimerView;
import com.affirm.android.widget.VCNCardView;

import java.util.Objects;

import static com.affirm.android.Affirm.RESULT_CHECKOUT_CANCEL;
import static com.affirm.android.Affirm.RESULT_CHECKOUT_EDIT_FROM_MERCHANT;
import static com.affirm.android.Affirm.RESULT_CHECKOUT_EDIT_FROM_NEW_FLOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_ID;
import static com.affirm.android.CardRequestType.CANCEL;
import static com.affirm.android.CardRequestType.EDIT;
import static com.affirm.android.CardRequestType.GET;

public class VcnDisplayActivity extends AppCompatActivity implements CardRequestCallback {

    private String checkoutId;
    private Checkout checkout;
    private CardRequest request;
    private CardDetails cardDetails;

    private VCNCardView vcnCardView;
    private TextView vcnShowMerchantName;
    private AppCompatButton vcnCopyCardNumber;
    private AffirmProgressBar indeterminateBar;
    private TextView vcnEditOrCancel;
    private CountDownTimerView vcnCountdown;

    public static void startActivity(@NonNull Activity activity, int requestCode,
                                     @NonNull String checkoutId,
                                     @Nullable Checkout checkout) {
        Intent intent = new Intent(activity, VcnDisplayActivity.class);
        intent.putExtra(CHECKOUT_ID, checkoutId);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AffirmUtils.showCloseActionBar(this);
        if (savedInstanceState != null) {
            checkoutId = savedInstanceState.getString(CHECKOUT_ID);
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
        } else {
            checkoutId = getIntent().getStringExtra(CHECKOUT_ID);
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
        }
        request = new CardRequest(Objects.requireNonNull(checkoutId), this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_vcn_display);
        vcnCardView = findViewById(R.id.vcnCardView);
        vcnShowMerchantName = findViewById(R.id.vcnShowMerchantName);
        indeterminateBar = findViewById(R.id.indeterminateBar);
        vcnCopyCardNumber = findViewById(R.id.vcnCopyCardNumber);
        vcnEditOrCancel = findViewById(R.id.vcnEditOrCancel);
        vcnCountdown = findViewById(R.id.vcnCountdown);

        vcnEditOrCancel.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.edit_amount_or_cancel_card);
            builder.setPositiveButton(R.string.edit_amount, (dialog, which) -> onEditCard());
            builder.setNegativeButton(R.string.cancel_card, (dialog, which) -> onCancelCard());
            builder.setNeutralButton(R.string.never_mind, (dialog, which) -> dialog.dismiss());
            builder.show();
        });

        vcnCopyCardNumber.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", cardDetails.number());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getApplicationContext(),
                    R.string.copy_to_clipboard_copied, Toast.LENGTH_SHORT).show();
        });

        onStartFetchCard();
    }

    @Override
    protected void onDestroy() {
        request.cancel();
        super.onDestroy();
    }

    private void onStartFetchCard() {
        startLoading();
        request.create(GET);
    }

    private void onCancelCard() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cancel_card);
        builder.setMessage(getString(R.string.cancel_card_message,
                AffirmPlugins.get().merchantName()));
        builder.setPositiveButton(R.string.cancel_card, (dialog, which) -> {
            startLoading();
            request.create(CANCEL);
        });
        builder.setNegativeButton(R.string.never_mind, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void onEditCard() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_amount_card);
        builder.setMessage(R.string.edit_amount_card_message);
        builder.setPositiveButton(R.string.edit_amount, (dialog, which) -> {
            startLoading();
            request.create(EDIT);
        });
        builder.setNegativeButton(R.string.never_mind, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CHECKOUT_ID, checkoutId);
        outState.putParcelable(CHECKOUT_EXTRA, checkout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_vcn_display, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finishWithCardDetails();
            return true;
        } else if (item.getItemId() == R.id.menu_question) {
            showQuestionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishWithCardDetails();
    }

    private void showQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.vcn_how_use_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.how_use_got_it).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void finishWithCardDetails() {
        if (cardDetails != null) {
            Intent intent = new Intent();
            intent.putExtra(AffirmConstants.CREDIT_DETAILS, cardDetails);
            setResult(Activity.RESULT_OK, intent);
        }
        finish();
    }

    @Override
    public void onError(@NonNull AffirmException exception) {
        endLoading();
        Toast.makeText(this, exception.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCardCancelSuccess(@NonNull CardCancelResponse response,
                                    @NonNull CardRequestType type) {
        endLoading();
        switch (type) {
            case CANCEL:
                setResult(RESULT_CHECKOUT_CANCEL);
                CardExpirationUtils.clearCachedCheckoutId(getApplicationContext());
                finish();
                break;
            case EDIT:
                if (checkout != null) {
                    // the page start from merchant, should restart the load amount page
                    Intent intent = new Intent();
                    intent.putExtra(CHECKOUT_EXTRA, checkout);
                    setResult(RESULT_CHECKOUT_EDIT_FROM_MERCHANT, intent);
                } else {
                    setResult(RESULT_CHECKOUT_EDIT_FROM_NEW_FLOW);
                }
                CardExpirationUtils.clearCachedCheckoutId(getApplicationContext());
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCardFetchSuccess(@NonNull CardDetails response, @NonNull CardRequestType type) {
        endLoading();
        vcnEditOrCancel.setVisibility(View.VISIBLE);
        vcnCopyCardNumber.setVisibility(View.VISIBLE);
        vcnCardView.setVisibility(View.VISIBLE);
        vcnCountdown.setVisibility(View.VISIBLE);

        cardDetails = response;
        vcnCardView.setVcn(cardDetails);
        vcnShowMerchantName.setText(AffirmPlugins.get().merchantName());
    }

    private void startLoading() {
        indeterminateBar.setVisibility(View.VISIBLE);
    }

    private void endLoading() {
        indeterminateBar.setVisibility(View.GONE);
    }
}


