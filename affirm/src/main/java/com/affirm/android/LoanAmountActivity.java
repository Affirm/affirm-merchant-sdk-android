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

import com.affirm.android.model.Checkout;
import com.affirm.android.widget.MoneyFormattedEditText;
import com.affirm.android.widget.NumericKeyboardView;

import org.joda.money.Money;

import static com.affirm.android.Affirm.RESULT_CHECKOUT_EDIT_FROM_NEW_FLOW;
import static com.affirm.android.AffirmConstants.CHECKOUT_CAAS_EXTRA;
import static com.affirm.android.AffirmConstants.CHECKOUT_EXTRA;

public class LoanAmountActivity extends AppCompatActivity {

    private Checkout checkout;
    private MoneyFormattedEditText loanAmountEditText;
    private String caas;

    public static void startActivity(@NonNull Activity activity, int requestCode,
                                     @NonNull Checkout checkout, @Nullable String caas) {
        Intent intent = new Intent(activity, LoanAmountActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        intent.putExtra(CHECKOUT_CAAS_EXTRA, caas);
        activity.startActivityForResult(intent, requestCode);
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
                Affirm.startVcnCheckout(this, checkout, caas, money, true);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_CHECKOUT_EDIT_FROM_NEW_FLOW) {
            setResult(resultCode, data);
            finish();
        }
    }
}
