package com.affirm.samples;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmCheckoutButton;
import com.affirm.android.AffirmCheckoutButtonShape;
import com.affirm.android.AffirmCheckoutButtonSize;
import com.affirm.android.AffirmCheckoutButtonTextType;
import com.affirm.android.AffirmCheckoutButtonTheme;
import com.affirm.android.AffirmUtils;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Item;
import com.affirm.android.model.PromoPageType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Starts an Express Checkout from an {@link AffirmCheckoutButton}. Affirm collects the shipping
 * address and gets shipping, tax and the total from the merchant's Shipping &amp; Totals endpoint.
 */
public class ExpressCheckoutActivity extends AppCompatActivity implements Affirm.CheckoutCallbacks {

    private static final BigDecimal PRICE = BigDecimal.valueOf(1000);
    private static final String STATE_CHECKOUT_IN_FLIGHT = "checkout_in_flight";

    private final List<AffirmCheckoutButton> buttons = new ArrayList<>();
    private EditText publicKeyInput;
    private EditText callbackUrlInput;
    private TextView result;
    private boolean checkoutInFlight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_express_checkout);
        publicKeyInput = findViewById(R.id.public_key);
        callbackUrlInput = findViewById(R.id.callback_url);
        result = findViewById(R.id.result);

        ((TextView) findViewById(R.id.environment)).setText(
                getString(R.string.render_demo_environment, BuildConfig.AFFIRM_ENVIRONMENT));
        publicKeyInput.setText(BuildConfig.EXPRESS_PUBLIC_KEY);
        callbackUrlInput.setText(BuildConfig.EXPRESS_CALLBACK_URL);

        startsCheckout(findViewById(R.id.xml_button));
        final AffirmCheckoutButton codeButton = addCodeButton(findViewById(R.id.code_button_container));
        addStyleToggles(findViewById(R.id.style_toggles), codeButton);

        // The checkout result arrives after a recreation, e.g. a rotation inside checkout.
        if (savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_CHECKOUT_IN_FLIGHT)) {
            setCheckoutInFlight(true);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_CHECKOUT_IN_FLIGHT, checkoutInFlight);
    }

    @NonNull
    private AffirmCheckoutButton addCodeButton(@NonNull ViewGroup container) {
        final AffirmCheckoutButton button = new AffirmCheckoutButton(this);
        button.setPageType(PromoPageType.PRODUCT);
        button.setSize(AffirmCheckoutButtonSize.MEDIUM);
        button.setTheme(AffirmCheckoutButtonTheme.DARK);
        button.setShape(AffirmCheckoutButtonShape.ROUNDED);
        button.setButtonText(AffirmCheckoutButtonTextType.CHECKOUT);
        container.addView(button, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        startsCheckout(button);
        return button;
    }

    private void addStyleToggles(@NonNull ViewGroup toggles, @NonNull AffirmCheckoutButton button) {
        addToggle(toggles, AffirmCheckoutButtonSize.values(), button.getSize(), button::setSize);
        addToggle(toggles, AffirmCheckoutButtonTheme.values(), button.getTheme(), button::setTheme);
        addToggle(toggles, AffirmCheckoutButtonShape.values(), button.getShape(), button::setShape);
        addToggle(toggles, AffirmCheckoutButtonTextType.values(), button.getButtonText(),
                button::setButtonText);

        final SwitchCompat fullWidth = new SwitchCompat(this);
        fullWidth.setText(R.string.express_demo_full_width);
        fullWidth.setChecked(button.isFullWidth());
        fullWidth.setOnCheckedChangeListener((v, checked) -> button.setFullWidth(checked));
        toggles.addView(fullWidth);
    }

    private interface OnSelected<T> {
        void onSelected(@NonNull T value);
    }

    private <T extends Enum<T>> void addToggle(@NonNull ViewGroup toggles, @NonNull T[] values,
                                               @NonNull T selected,
                                               @NonNull OnSelected<T> onSelected) {
        final RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);
        for (T value : values) {
            final RadioButton option = new RadioButton(this);
            // RadioGroup only keeps a single option checked among children with ids.
            option.setId(View.generateViewId());
            option.setText(value.name());
            option.setChecked(value == selected);
            option.setOnClickListener(v -> onSelected.onSelected(value));
            group.addView(option);
        }
        // Scrolls at large font scales instead of squeezing the last option into a column.
        final HorizontalScrollView row = new HorizontalScrollView(this);
        row.addView(group);
        toggles.addView(row);
    }

    private void startsCheckout(@NonNull AffirmCheckoutButton button) {
        button.setOnClickListener(v -> startExpressCheckout());
        buttons.add(button);
    }

    private void startExpressCheckout() {
        final String publicKey = publicKeyInput.getText().toString().trim();
        if (publicKey.isEmpty()) {
            publicKeyInput.setError(getString(R.string.express_demo_public_key_missing));
            return;
        }

        final Checkout checkout;
        try {
            checkout = Checkout.createExpressCheckout(items(),
                    "express-demo-" + System.currentTimeMillis(),
                    callbackUrlInput.getText().toString().trim(), metadata());
        } catch (IllegalStateException e) {
            showResult(getString(R.string.express_demo_error, e.getMessage()));
            return;
        }

        // Express Checkout is only available in the US.
        Affirm.setPublicKey(publicKey);
        Affirm.setCountryCode("USA");
        Affirm.setLocale("en_US");

        // The buttons don't disable themselves; keep them disabled until checkout returns.
        setCheckoutInFlight(true);
        showResult(getString(R.string.express_demo_started));
        Affirm.startCheckout(this, checkout, false);
    }

    @NonNull
    private static Map<String, Item> items() {
        final Item item = Item.builder()
                .setDisplayName("Great Deal Wheel")
                .setImageUrl("http://www.m2motorsportinc.com/media/catalog/product/cache/1/thumbnail"
                        + "/9df78eab33525d08d6e5fb8d27136e95/v/e/velocity-vw125-wheels-rims.jpg")
                .setQty(1)
                .setSku("wheel")
                .setUnitPrice(PRICE)
                .setUrl("http://merchant.com/great_deal_wheel")
                .build();
        return Collections.singletonMap("wheel", item);
    }

    @NonNull
    private static Map<String, String> metadata() {
        // The subtotal of the items, in cents, as a string.
        return Collections.singletonMap("subtotal",
                String.valueOf(AffirmUtils.decimalDollarsToIntegerCents(PRICE)));
    }

    private void setCheckoutInFlight(boolean inFlight) {
        checkoutInFlight = inFlight;
        for (AffirmCheckoutButton button : buttons) {
            button.setEnabled(!inFlight);
        }
    }

    private void showResult(@NonNull String text) {
        result.setText(text);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Affirm.handleCheckoutData(this, requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // - Affirm.CheckoutCallbacks
    @Override
    public void onAffirmCheckoutSuccess(@NonNull String token) {
        setCheckoutInFlight(false);
        showResult(getString(R.string.express_demo_success, token));
    }

    @Override
    public void onAffirmCheckoutCancelled() {
        setCheckoutInFlight(false);
        showResult(getString(R.string.express_demo_cancelled));
    }

    @Override
    public void onAffirmCheckoutError(@Nullable String message) {
        setCheckoutInFlight(false);
        showResult(getString(R.string.express_demo_error, message));
    }
}
