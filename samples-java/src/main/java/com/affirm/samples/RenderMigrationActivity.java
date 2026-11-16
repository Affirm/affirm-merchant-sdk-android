package com.affirm.samples;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmColor;
import com.affirm.android.AffirmLogoType;
import com.affirm.android.AffirmPromotionButton;
import com.affirm.android.model.PromoPageType;

import java.math.BigDecimal;

/**
 * Shows the native-to-HTML promo render migration: a native configure call rendered as native
 * text or as HTML.
 */
public class RenderMigrationActivity extends AppCompatActivity {

    // The SDK has no getters for these, so the demo keeps what it last loaded. The key and the
    // country stay set in the SDK after leaving, so other screens use the loaded merchant.
    private static String publicKey = Config.PUBLIC_KEY;
    private static String countryCode = Config.COUNTRY_CODE;
    private static BigDecimal amount = BigDecimal.valueOf(1100);

    private RadioGroup country;
    private RadioGroup font;
    private RadioGroup color;
    private SwitchCompat optOut;
    private EditText publicKeyInput;
    private EditText amountInput;
    private TextView loaded;
    private AffirmPromotionButton nativeApiPromo;
    private TextView nativeApiStatus;
    private LogPanel logPanel;
    private final ViewTreeObserver.OnPreDrawListener statusUpdater = () -> {
        // A promo changes without a callback, e.g. its HTML becomes visible when ready.
        showStatus(nativeApiPromo, nativeApiStatus);
        return true;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render_migration);
        country = findViewById(R.id.country);
        font = findViewById(R.id.font);
        color = findViewById(R.id.color);
        optOut = findViewById(R.id.opt_out);
        publicKeyInput = findViewById(R.id.public_key);
        amountInput = findViewById(R.id.amount);
        loaded = findViewById(R.id.loaded);
        nativeApiPromo = findViewById(R.id.native_api_promo);
        nativeApiStatus = findViewById(R.id.native_api_status);
        final TextView log = findViewById(R.id.log);
        logPanel = new LogPanel(log);

        ((TextView) findViewById(R.id.environment)).setText(
                getString(R.string.render_demo_environment, BuildConfig.AFFIRM_ENVIRONMENT));
        publicKeyInput.setText(publicKey);
        amountInput.setText(amount.toPlainString());
        country.check(countryButton(countryCode));
        findViewById(R.id.load).setOnClickListener(v -> load());
        findViewById(R.id.copy_log).setOnClickListener(v -> {
            final ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Affirm SDK log", log.getText()));
        });

        optOut.setOnCheckedChangeListener((button, checked) -> {
            Affirm.setUseLegacyNativeRendering(checked);
            reload();
        });
        font.setOnCheckedChangeListener((group, checkedId) -> reload());
        color.setOnCheckedChangeListener((group, checkedId) -> reload());
        getWindow().getDecorView().getViewTreeObserver().addOnPreDrawListener(statusUpdater);
        reload();
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
        getWindow().getDecorView().getViewTreeObserver().removeOnPreDrawListener(statusUpdater);
        logPanel.release();
        if (isFinishing()) {
            // Leave the rest of the sample app with the configured opt-out.
            Affirm.setUseLegacyNativeRendering(false);
        }
        super.onDestroy();
    }

    private void load() {
        final String key = publicKeyInput.getText().toString().trim();
        final BigDecimal newAmount = parseAmount(amountInput.getText().toString());
        if (key.isEmpty()) {
            publicKeyInput.setError(getString(R.string.render_demo_public_key_missing));
            return;
        }
        if (newAmount == null) {
            amountInput.setError(getString(R.string.render_demo_amount_invalid));
            return;
        }
        final String newCountryCode = countryCodeOf(country.getCheckedRadioButtonId());
        publicKey = key;
        amount = newAmount;
        Affirm.setPublicKey(key);
        if (!newCountryCode.equals(countryCode)) {
            countryCode = newCountryCode;
            Affirm.setCountryCode(countryCode);
            Affirm.setLocale(localeOf(countryCode));
        }
        reload();
    }

    private static int countryButton(@NonNull String countryCode) {
        switch (countryCode) {
            case "USA":
                return R.id.country_usa;
            case "CAN":
                return R.id.country_can;
            default:
                return R.id.country_gbr;
        }
    }

    @NonNull
    private static String countryCodeOf(int countryButton) {
        if (countryButton == R.id.country_usa) {
            return "USA";
        } else if (countryButton == R.id.country_can) {
            return "CAN";
        }
        return "GBR";
    }

    @NonNull
    private static String localeOf(@NonNull String countryCode) {
        switch (countryCode) {
            case "USA":
                return "en_US";
            case "CAN":
                return "en_CA";
            default:
                return "en_GB";
        }
    }

    @Nullable
    private static BigDecimal parseAmount(@NonNull String text) {
        try {
            final BigDecimal parsed = new BigDecimal(text.trim());
            return parsed.signum() > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void reload() {
        loaded.setText(context() + " · useLegacyNativeRendering " + optOut.isChecked());
        LogPanel.mark(context() + " · font " + selectedFontName()
                + " · useLegacyNativeRendering " + optOut.isChecked());
        nativeApiPromo.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, selectedTypeface(), selectedColor(),
                R.dimen.render_demo_text_size);
        Affirm.configureWithAmount(nativeApiPromo, PromoPageType.PRODUCT, amount, true);
    }

    @NonNull
    private static String context() {
        return BuildConfig.AFFIRM_ENVIRONMENT + " · " + countryCode + " · " + publicKey + " · "
                + amount.toPlainString();
    }

    private Typeface selectedTypeface() {
        if (font.getCheckedRadioButtonId() == R.id.font_custom) {
            return ResourcesCompat.getFont(this, R.font.lacquer_regular);
        }
        return Typeface.DEFAULT;
    }

    @NonNull
    private String selectedFontName() {
        final TextView checked = findViewById(font.getCheckedRadioButtonId());
        return checked.getText().toString();
    }

    private int selectedColor() {
        final int checked = color.getCheckedRadioButtonId();
        if (checked == R.id.color_indigo) {
            return R.color.renderDemoIndigo;
        } else if (checked == R.id.color_black) {
            return android.R.color.black;
        }
        return R.color.renderDemoGray;
    }

    private static void showStatus(AffirmPromotionButton promo, TextView status) {
        final String text = "Shown as: " + PromoViewState.shownAs(promo);
        // Setting the same text again would lay out and draw again, every frame.
        if (!TextUtils.equals(text, status.getText())) {
            status.setText(text);
        }
    }
}
