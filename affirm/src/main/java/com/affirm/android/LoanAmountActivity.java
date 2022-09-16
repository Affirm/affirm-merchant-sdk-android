package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.affirm.android.model.Checkout;
import com.affirm.android.widget.MoneyFormattedEditText;
import com.affirm.android.widget.NumericKeyboardView;

import org.joda.money.Money;

import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_CARD_AUTH_WINDOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;

public class LoanAmountActivity extends AppCompatActivity {

    private Checkout checkout;
    private MoneyFormattedEditText loanAmountEditText;
    private String caas;
    private int cardAuthWindow;

    public static void startActivity(@NonNull Activity activity, int requestCode,
                                     @NonNull Checkout checkout, @Nullable String caas,
                                     int cardAuthWindow) {
        Intent intent = buildIntent(activity, checkout, caas, cardAuthWindow);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startActivity(@NonNull Fragment fragment, int requestCode,
                                     @NonNull Checkout checkout, @Nullable String caas,
                                     int cardAuthWindow) {
        Intent intent = buildIntent(fragment.requireActivity(), checkout, caas, cardAuthWindow);
        fragment.startActivityForResult(intent, requestCode);
    }

    private static Intent buildIntent(
            @NonNull Activity originalActivity,
            @NonNull Checkout checkout,
            @Nullable String caas,
            int cardAuthWindow) {
        Intent intent = new Intent(originalActivity, LoanAmountActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        intent.putExtra(CHECKOUT_CAAS_EXTRA, caas);
        intent.putExtra(CHECKOUT_CARD_AUTH_WINDOW, cardAuthWindow);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            checkout = savedInstanceState.getParcelable(CHECKOUT_EXTRA);
            caas = savedInstanceState.getString(CHECKOUT_CAAS_EXTRA);
            cardAuthWindow = savedInstanceState.getInt(CHECKOUT_CARD_AUTH_WINDOW, -1);
        } else {
            checkout = getIntent().getParcelableExtra(CHECKOUT_EXTRA);
            caas = getIntent().getStringExtra(CHECKOUT_CAAS_EXTRA);
            cardAuthWindow = getIntent().getIntExtra(CHECKOUT_CARD_AUTH_WINDOW, -1);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loan_amount);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.affirm_ic_baseline_close_black);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        NumericKeyboardView numericKeyboardView = findViewById(R.id.keyboard);
        loanAmountEditText = findViewById(R.id.loanAmountEditText);
        loanAmountEditText.setText(String.valueOf(checkout.total() / 100));
        numericKeyboardView.setTarget(loanAmountEditText);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CHECKOUT_EXTRA, checkout);
        outState.putString(CHECKOUT_CAAS_EXTRA, caas);
        outState.putInt(CHECKOUT_CARD_AUTH_WINDOW, cardAuthWindow);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_loan_amount, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.menu_next) {
            Money money = loanAmountEditText.getRaw();
            if (!money.isZero()) {
                Affirm.startVcnCheckout(this, checkout, caas, money, true,
                        cardAuthWindow);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setResult(resultCode, data);
        finish();
    }
}
