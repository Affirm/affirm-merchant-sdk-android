package com.affirm.android;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.affirm.android.model.CardDetailsInner;
import com.affirm.android.model.Checkout;
import com.affirm.android.widget.CountDownTimerView;
import com.affirm.android.widget.VCNCardView;

import java.util.Locale;

import static com.affirm.android.Affirm.RESULT_CHECKOUT_CANCEL;
import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;
import static com.affirm.android.AffirmConstants.CREDIT_DETAILS;

public class VcnDisplayActivity extends AppCompatActivity {

    private Checkout checkout;
    private String caas;

    public static void startActivity(@NonNull Activity activity, int requestCode,
                                     @NonNull Checkout checkout, @Nullable String caas) {
        Intent intent = buildIntent(activity, checkout, caas);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startActivity(@NonNull Fragment fragment, int requestCode,
                                     @NonNull Checkout checkout, @Nullable String caas) {
        Intent intent = buildIntent(fragment.requireActivity(), checkout, caas);
        fragment.startActivityForResult(intent, requestCode);
    }

    private static Intent buildIntent(
            @NonNull Activity originalActivity,
            @NonNull Checkout checkout,
            @Nullable String caas) {
        Intent intent = new Intent(originalActivity, VcnDisplayActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        intent.putExtra(CHECKOUT_CAAS_EXTRA, caas);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
            caas = savedInstanceState.getString(CHECKOUT_CAAS_EXTRA);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
            caas = getIntent().getStringExtra(CHECKOUT_CAAS_EXTRA);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vcn_display);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(getResources().getColor(R.color.affirm_color_primary));
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.affirm_ic_baseline_close_black);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(AffirmPlugins.get().merchantName());
        }

        TextView vcnShowAmount = findViewById(R.id.vcnShowAmount);
        TextView vcnCardTip = findViewById(R.id.vcnCardTip);
        VCNCardView vcnCardView = findViewById(R.id.vcnCardView);
        AppCompatButton vcnCopyCardNumber = findViewById(R.id.vcnCopyCardNumber);
        TextView vcnEditOrCancel = findViewById(R.id.vcnEditOrCancel);
        CountDownTimerView vcnCountdown = findViewById(R.id.vcnCountdown);

        vcnShowAmount.setText(String.format(Locale.ROOT, "$%.2f", checkout.total() / 100f));

        String cardTip = AffirmPlugins.get().cardTip();
        if (!TextUtils.isEmpty(cardTip)) {
            vcnCardTip.setText(cardTip);
            vcnCardTip.setVisibility(View.VISIBLE);
        } else {
            vcnCardTip.setVisibility(View.GONE);
        }

        CardDetailsInner cardDetailsInner = AffirmPlugins.get().getCachedCardDetails();
        vcnCardView.setVcn(cardDetailsInner.getCardDetails());
        vcnCountdown.start(cardDetailsInner.getExpirationDate());

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
            ClipData clip = ClipData.newPlainText("", cardDetailsInner.getCardDetails().number());
            clipboard.setPrimaryClip(clip);
            finishWithCardDetails();
        });
    }

    private void onCancelCard() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.cancel_card);
        builder.setMessage(getString(R.string.cancel_card_message,
                AffirmPlugins.get().merchantName()));
        builder.setPositiveButton(R.string.cancel_card, (dialog, which) -> {
            onCancellation();
        });
        builder.setNegativeButton(R.string.never_mind, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void onEditCard() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_amount_card);
        builder.setMessage(R.string.edit_amount_card_message);
        builder.setPositiveButton(R.string.edit_amount, (dialog, which) -> {
            Affirm.startNewVcnCheckoutFlow(this, checkout, caas);
        });
        builder.setNegativeButton(R.string.never_mind, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CHECKOUT_EXTRA, checkout);
        outState.putString(CHECKOUT_CAAS_EXTRA, caas);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_vcn_display, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onCancellation();
            return true;
        } else if (item.getItemId() == R.id.menu_question) {
            showQuestionDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showQuestionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.vcn_how_use_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.how_use_got_it).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void onCancellation() {
        setResult(RESULT_CHECKOUT_CANCEL);
        finish();
    }

    private void finishWithCardDetails() {
        Intent intent = new Intent();
        intent.putExtra(CREDIT_DETAILS,
                AffirmPlugins.get().getCachedCardDetails().getCardDetails());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }
}


