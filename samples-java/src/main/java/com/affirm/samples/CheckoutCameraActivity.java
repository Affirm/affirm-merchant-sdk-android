package com.affirm.samples;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.affirm.android.Affirm;
import com.affirm.android.model.Address;
import com.affirm.android.model.AffirmError;
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
 * Starts a checkout so the checkout WebView can ask for the camera and microphone, as the UK
 * identity verification step does. Shows the app's permission state, the checkout result and the
 * SDK's permission log.
 */
public class CheckoutCameraActivity extends AppCompatActivity
        implements Affirm.CheckoutCallbacks, Affirm.VcnCheckoutCallbacks,
        EntrustDemoLoader.Listener {

    private static final String[] MEDIA_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
    };
    private static final BigDecimal PRICE = BigDecimal.valueOf(1100);
    // The document step asks for the camera; motion with recordMotionAudio also asks for the
    // microphone.
    private static final String ENTRUST_DEMO_URL =
            "https://sdk.onfido.com/demo?scenario=classic-doc---motion";

    // The SDK only grants camera requests on uk.affirm.com pages, so the demo checks out in the UK.
    private static final String COUNTRY_CODE = "GBR";
    private static final String LOCALE = "en_GB";
    private static final Address ADDRESS = Address.builder()
            .setStreet1("1 Oxford Street")
            .setCity("London")
            .setRegion1Code("London")
            .setPostalCode("W1D 1AN")
            .setCountry(COUNTRY_CODE)
            .build();

    // The SDK has no getter for it; it stays set after leaving, like the render demo.
    private static String publicKey = Config.PUBLIC_KEY;

    private TextView permissionStatus;
    private EditText publicKeyInput;
    private TextView result;
    private LogPanel logPanel;
    private final EntrustDemoLoader entrustDemo = new EntrustDemoLoader(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_camera);
        permissionStatus = findViewById(R.id.permission_status);
        publicKeyInput = findViewById(R.id.public_key);
        result = findViewById(R.id.result);
        final TextView log = findViewById(R.id.log);
        logPanel = new LogPanel(log);

        ((TextView) findViewById(R.id.environment)).setText(getString(
                R.string.camera_demo_environment, BuildConfig.AFFIRM_ENVIRONMENT));
        publicKeyInput.setText(publicKey);

        findViewById(R.id.app_settings).setOnClickListener(v -> startActivity(
                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", getPackageName(), null))));
        findViewById(R.id.checkout).setOnClickListener(v -> {
            entrustDemo.disarm();
            start(false);
        });
        findViewById(R.id.vcn_checkout).setOnClickListener(v -> {
            entrustDemo.disarm();
            start(true);
        });
        findViewById(R.id.entrust_demo).setOnClickListener(v -> {
            entrustDemo.arm(ENTRUST_DEMO_URL);
            if (start(false)) {
                result.setText(R.string.camera_demo_entrust_waiting);
            } else {
                entrustDemo.disarm();
            }
        });
        getApplication().registerActivityLifecycleCallbacks(entrustDemo);
        findViewById(R.id.copy_log).setOnClickListener(v -> {
            final ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Affirm SDK log", log.getText()));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The user may have changed the permissions in the system settings.
        showPermissionStatus();
        logPanel.start();
    }

    @Override
    protected void onPause() {
        logPanel.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        entrustDemo.disarm();
        getApplication().unregisterActivityLifecycleCallbacks(entrustDemo);
        logPanel.release();
        super.onDestroy();
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

    /** Returns false when the checkout could not start. */
    private boolean start(boolean useVcn) {
        final String key = publicKeyInput.getText().toString().trim();
        if (key.isEmpty()) {
            publicKeyInput.setError(getString(R.string.render_demo_public_key_missing));
            return false;
        }
        publicKey = key;
        Affirm.setPublicKey(key);
        Affirm.setCountryCode(COUNTRY_CODE);
        Affirm.setLocale(LOCALE);

        final String name = getString(useVcn ? R.string.vcn_checkout : R.string.checkout);
        LogPanel.mark("Start " + name);
        result.setText(getString(R.string.camera_demo_started, name));
        try {
            Affirm.startCheckout(this, checkout(), null, -1, useVcn);
            return true;
        } catch (Exception e) {
            showResult(getString(R.string.camera_demo_failed, e.getMessage()));
            return false;
        }
    }

    @Override
    public void onEntrustDemoLoaded(@NonNull String url) {
        result.setText(getString(R.string.camera_demo_entrust_loaded, url));
    }

    @Override
    public void onEntrustDemoTimeout() {
        result.setText(R.string.camera_demo_entrust_timeout);
    }

    private void showResult(@NonNull String text) {
        entrustDemo.disarm();
        result.setText(text);
        showPermissionStatus();
    }

    private void showPermissionStatus() {
        final StringBuilder status = new StringBuilder();
        for (String permission : MEDIA_PERMISSIONS) {
            if (status.length() > 0) {
                status.append('\n');
            }
            final boolean granted = ContextCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED;
            status.append(getString(R.string.camera_demo_permission_line,
                    permission.substring(permission.lastIndexOf('.') + 1),
                    getString(granted
                            ? R.string.camera_demo_granted : R.string.camera_demo_not_granted)));
        }
        permissionStatus.setText(status);
    }

    @NonNull
    private static Checkout checkout() {
        final Item item = Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl("https://www.affirm.com/favicon.ico")
                .setQty(1)
                .setSku("wheel")
                .setUnitPrice(BigDecimal.valueOf(1000))
                .setUrl("https://merchant.com/great_deal_wheel")
                .build();

        final Name name = Name.builder().setFull("John Smith").build();

        return Checkout.builder()
                .setOrderId("camera-demo")
                .setItems(Collections.singletonMap("wheel", item))
                .setBilling(Billing.builder().setAddress(ADDRESS).setName(name).build())
                .setShipping(Shipping.builder().setAddress(ADDRESS).setName(name).build())
                .setShippingAmount(BigDecimal.ZERO)
                .setTaxAmount(BigDecimal.valueOf(100))
                .setTotal(PRICE)
                .setCurrency(Currency.GBP)
                .build();
    }

    @Override
    public void onAffirmCheckoutSuccess(@NonNull String token) {
        showResult(getString(R.string.camera_demo_success, token));
    }

    @Override
    public void onAffirmCheckoutCancelled() {
        showResult(getString(R.string.camera_demo_cancelled));
    }

    @Override
    public void onAffirmCheckoutError(@Nullable String message) {
        showResult(getString(R.string.camera_demo_error, message));
    }

    @Override
    public void onAffirmCheckoutError(@Nullable String message, @Nullable AffirmError error) {
        showResult(getString(R.string.camera_demo_error, message));
    }

    @Override
    public void onAffirmVcnCheckoutSuccess(@NonNull CardDetails cardDetails) {
        final String number = cardDetails.number();
        final String last4 = number != null && number.length() > 4
                ? number.substring(number.length() - 4) : String.valueOf(number);
        showResult(getString(R.string.camera_demo_vcn_success, last4));
    }

    @Override
    public void onAffirmVcnCheckoutCancelled() {
        showResult(getString(R.string.camera_demo_cancelled));
    }

    @Override
    public void onAffirmVcnCheckoutCancelledReason(@NonNull VcnReason vcnReason) {
        showResult(getString(R.string.camera_demo_vcn_cancelled_reason, vcnReason.reason()));
    }

    @Override
    public void onAffirmVcnCheckoutError(@Nullable String message) {
        showResult(getString(R.string.camera_demo_error, message));
    }
}
