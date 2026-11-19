package com.affirm.samples;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.affirm.android.Affirm;
import com.affirm.android.model.Address;
import com.affirm.android.model.AffirmPrepareAuthResult;
import com.affirm.android.model.Billing;
import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Currency;
import com.affirm.android.model.Item;
import com.affirm.android.model.Name;
import com.affirm.android.model.Shipping;
import com.affirm.android.model.VcnReason;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * Connected Accounts: prepareAuth, the merchant server's session code exchange, then a checkout
 * with the session code. The mock server's session codes are rejected by Affirm, so those
 * checkouts still ask the customer to sign in; Manual takes a real session code instead.
 */
public class ConnectedAccountsActivity extends AppCompatActivity
        implements Affirm.CheckoutCallbacks, Affirm.VcnCheckoutCallbacks {

    private static final BigDecimal PRICE = BigDecimal.valueOf(1100.0);

    // Connected Accounts is US only.
    private static final String COUNTRY_CODE = "USA";
    private static final String LOCALE = "en_US";

    private final MockMerchantBackend backend = new MockMerchantBackend();

    private EditText publicKeyInput;
    private EditText accountLinkingIdInput;
    private RadioGroup backendMode;
    private View manualSection;
    private TextView codeChallengeView;
    private EditText sessionCodeInput;
    private LogPanel logPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_accounts);
        publicKeyInput = findViewById(R.id.public_key);
        accountLinkingIdInput = findViewById(R.id.account_linking_id);
        backendMode = findViewById(R.id.backend_mode);
        manualSection = findViewById(R.id.manual);
        codeChallengeView = findViewById(R.id.code_challenge);
        sessionCodeInput = findViewById(R.id.session_code);
        final TextView log = findViewById(R.id.log);
        logPanel = new LogPanel(log);

        ((TextView) findViewById(R.id.environment)).setText(
                getString(R.string.render_demo_environment, BuildConfig.AFFIRM_ENVIRONMENT));
        backendMode.setOnCheckedChangeListener((group, checkedId) -> updateManualSection());
        findViewById(R.id.get_challenge).setOnClickListener(v -> showCodeChallenge());
        findViewById(R.id.copy_challenge).setOnClickListener(v ->
                copy("Code challenge", codeChallengeView.getText()));
        findViewById(R.id.checkout).setOnClickListener(v -> start(false));
        findViewById(R.id.vcn_checkout).setOnClickListener(v -> start(true));
        findViewById(R.id.copy_log).setOnClickListener(v ->
                copy("Affirm SDK log", log.getText()));
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        updateManualSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        logPanel.start();
    }

    @Override
    protected void onPause() {
        logPanel.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        backend.cancel();
        logPanel.release();
        if (isFinishing()) {
            // Leave the rest of the sample app with its configured merchant.
            Affirm.setPublicKey(Config.PUBLIC_KEY);
            Affirm.setCountryCode(Config.COUNTRY_CODE);
            Affirm.setLocale(Config.LOCALE);
        }
        super.onDestroy();
    }

    private void updateManualSection() {
        manualSection.setVisibility(isManual() ? View.VISIBLE : View.GONE);
    }

    private boolean isManual() {
        return backendMode.getCheckedRadioButtonId() == R.id.backend_manual;
    }

    private void showCodeChallenge() {
        Affirm.prepareAuth(result -> {
            final String codeChallenge = result.codeChallenge();
            codeChallengeView.setText(codeChallenge);
            LogPanel.note("prepareAuth: " + (codeChallenge != null
                    ? "code challenge " + codeChallenge : "unavailable"));
        });
    }

    private void copy(@NonNull String label, @NonNull CharSequence text) {
        final ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
    }

    private void start(boolean useVCN) {
        final String publicKey = publicKeyInput.getText().toString().trim();
        if (publicKey.isEmpty()) {
            publicKeyInput.setError(getString(R.string.connected_accounts_public_key_missing));
            return;
        }
        final String accountLinkingId = accountLinkingIdInput.getText().toString().trim();
        if (accountLinkingId.isEmpty()) {
            accountLinkingIdInput.setError(getString(R.string.connected_accounts_id_missing));
            return;
        }
        final String manualSessionCode = sessionCodeInput.getText().toString().trim();
        if (isManual() && manualSessionCode.isEmpty()) {
            sessionCodeInput.setError(getString(R.string.connected_accounts_session_code_missing));
            return;
        }
        Affirm.setPublicKey(publicKey);
        Affirm.setCountryCode(COUNTRY_CODE);
        Affirm.setLocale(LOCALE);
        backend.cancel();

        if (isManual()) {
            LogPanel.mark("Connected Accounts · " + publicKey + " · " + accountLinkingId
                    + " · manual session code" + (useVCN ? " · VCN" : ""));
            startManualCheckout(accountLinkingId, useVCN, manualSessionCode);
            return;
        }
        final MockMerchantBackend.Mode mode = selectedMode();
        backend.setMode(mode);
        LogPanel.mark("Connected Accounts · " + publicKey + " · " + accountLinkingId
                + " · backend " + mode + (useVCN ? " · VCN" : ""));

        Affirm.prepareAuth(result -> {
            if (result.status() != AffirmPrepareAuthResult.Status.PENDING) {
                LogPanel.note("prepareAuth: unavailable, checking out without a session code");
                startCheckout(accountLinkingId, useVCN, null);
                return;
            }
            final String codeChallenge = result.codeChallenge();
            LogPanel.note("prepareAuth: code challenge " + codeChallenge);
            backend.requestSessionCode(accountLinkingId, codeChallenge, (sessionCode, detail) -> {
                LogPanel.note("Merchant backend: " + detail);
                startCheckout(accountLinkingId, useVCN, sessionCode);
            });
        });
    }

    private void startManualCheckout(@NonNull String accountLinkingId, boolean useVCN,
                                     @NonNull String sessionCode) {
        Affirm.prepareAuth(result -> {
            final String codeChallenge = result.codeChallenge();
            if (codeChallenge == null) {
                LogPanel.note("prepareAuth: unavailable, the session code will be ignored");
            } else if (!codeChallenge.contentEquals(codeChallengeView.getText())) {
                LogPanel.note("prepareAuth: code challenge " + codeChallenge + " is not the one "
                        + "shown, so Affirm will reject the session code. Get a new challenge.");
            }
            // Both are single use: the next checkout needs a new challenge and session code.
            codeChallengeView.setText(null);
            sessionCodeInput.setText(null);
            startCheckout(accountLinkingId, useVCN, sessionCode);
        });
    }

    private void startCheckout(@NonNull String accountLinkingId, boolean useVCN,
                               @Nullable String sessionCode) {
        LogPanel.note("startCheckout: session code " + (sessionCode != null ? sessionCode : "none"));
        Affirm.startCheckout(this, checkout(accountLinkingId), null, -1, useVCN, sessionCode);
    }

    @NonNull
    private MockMerchantBackend.Mode selectedMode() {
        final int checked = backendMode.getCheckedRadioButtonId();
        if (checked == R.id.backend_not_linked) {
            return MockMerchantBackend.Mode.NOT_LINKED;
        } else if (checked == R.id.backend_token_rejected) {
            return MockMerchantBackend.Mode.TOKEN_REJECTED;
        }
        return MockMerchantBackend.Mode.LINKED;
    }

    @NonNull
    private static Checkout checkout(@NonNull String accountLinkingId) {
        final Item item = Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl("http://www.m2motorsportinc.com/media/catalog/product/cache/1/"
                        + "thumbnail/9df78eab33525d08d6e5fb8d27136e95/v/e/"
                        + "velocity-vw125-wheels-rims.jpg")
                .setQty(1)
                .setSku("wheel")
                .setUnitPrice(BigDecimal.valueOf(1000.0))
                .setUrl("http://merchant.com/great_deal_wheel")
                .build();
        final Name name = Name.builder().setFull("John Smith").build();
        final Address address = Address.builder()
                .setCity("San Francisco")
                .setCountry(COUNTRY_CODE)
                .setStreet1("333 Kansas st")
                .setRegion1Code("CA")
                .setPostalCode("94107")
                .build();

        return Checkout.builder()
                .setOrderId("55555")
                .setItems(Collections.singletonMap("wheel", item))
                .setBilling(Billing.builder().setAddress(address).setName(name).build())
                .setShipping(Shipping.builder().setAddress(address).setName(name).build())
                .setShippingAmount(BigDecimal.valueOf(0.0))
                .setTaxAmount(BigDecimal.valueOf(100.0))
                .setTotal(PRICE)
                .setCurrency(Currency.USD)
                .setAccountLinkingId(accountLinkingId)
                .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Affirm.handleCheckoutData(this, requestCode, resultCode, data)) {
            return;
        }
        if (Affirm.handleVcnCheckoutData(this, requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // - Affirm.CheckoutCallbacks
    @Override
    public void onAffirmCheckoutSuccess(@NonNull String token) {
        LogPanel.note("Checkout succeeded, token " + token);
    }

    @Override
    public void onAffirmCheckoutCancelled() {
        LogPanel.note("Checkout cancelled");
    }

    @Override
    public void onAffirmCheckoutError(@Nullable String message) {
        LogPanel.note("Checkout error: " + message);
    }

    // - Affirm.VcnCheckoutCallbacks
    @Override
    public void onAffirmVcnCheckoutSuccess(@NonNull CardDetails cardDetails) {
        LogPanel.note("VCN checkout succeeded");
    }

    @Override
    public void onAffirmVcnCheckoutCancelled() {
        LogPanel.note("VCN checkout cancelled");
    }

    @Override
    public void onAffirmVcnCheckoutCancelledReason(@NonNull VcnReason vcnReason) {
        LogPanel.note("VCN checkout cancelled: " + vcnReason);
    }

    @Override
    public void onAffirmVcnCheckoutError(@Nullable String message) {
        LogPanel.note("VCN checkout error: " + message);
    }
}
