package com.affirm.samples;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
 * Shows the native-to-HTML promo render migration: the same native configure call rendered as
 * native text or as HTML, next to an explicit HTML call.
 */
public class RenderMigrationActivity extends AppCompatActivity {

    private static final BigDecimal PRICE = BigDecimal.valueOf(1100.0);

    private RadioGroup font;
    private RadioGroup color;
    private AffirmPromotionButton nativeApiPromo;
    private AffirmPromotionButton explicitHtmlPromo;
    private TextView nativeApiStatus;
    private TextView explicitHtmlStatus;
    private final ViewTreeObserver.OnPreDrawListener statusUpdater = () -> {
        // A promo changes without a callback, e.g. its HTML becomes visible when ready.
        showStatus(nativeApiPromo, nativeApiStatus);
        showStatus(explicitHtmlPromo, explicitHtmlStatus);
        return true;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render_migration);
        font = findViewById(R.id.font);
        color = findViewById(R.id.color);
        nativeApiPromo = findViewById(R.id.native_api_promo);
        explicitHtmlPromo = findViewById(R.id.explicit_html_promo);
        nativeApiStatus = findViewById(R.id.native_api_status);
        explicitHtmlStatus = findViewById(R.id.explicit_html_status);

        final SwitchCompat optOut = findViewById(R.id.opt_out);
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
    protected void onDestroy() {
        getWindow().getDecorView().getViewTreeObserver().removeOnPreDrawListener(statusUpdater);
        if (isFinishing()) {
            // Leave the rest of the sample app with the configured opt-out.
            Affirm.setUseLegacyNativeRendering(false);
        }
        super.onDestroy();
    }

    private void reload() {
        nativeApiPromo.configWithLocalStyling(AffirmColor.AFFIRM_COLOR_TYPE_BLUE,
                AffirmLogoType.AFFIRM_DISPLAY_TYPE_LOGO, selectedTypeface(), selectedColor(),
                R.dimen.render_demo_text_size);
        Affirm.configureWithAmount(nativeApiPromo, PromoPageType.PRODUCT, PRICE, true);
        explicitHtmlPromo.configWithHtmlStyling(null, null);
        Affirm.configureWithAmount(explicitHtmlPromo, PromoPageType.PRODUCT, PRICE, true);
    }

    private Typeface selectedTypeface() {
        final int checked = font.getCheckedRadioButtonId();
        if (checked == R.id.font_bold) {
            return Typeface.DEFAULT_BOLD;
        } else if (checked == R.id.font_serif_italic) {
            return Typeface.create(Typeface.SERIF, Typeface.ITALIC);
        } else if (checked == R.id.font_mono) {
            return Typeface.MONOSPACE;
        } else if (checked == R.id.font_custom) {
            return ResourcesCompat.getFont(this, R.font.lacquer_regular);
        }
        return Typeface.DEFAULT;
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
        String text = "Shown as: " + PromoViewState.shownAs(promo);
        if (promo.getVisibility() == View.VISIBLE) {
            text += "\nTalkBack: \"" + PromoViewState.talkBackLabel(promo) + "\"";
        }
        // Setting the same text again would lay out and draw again, every frame.
        if (!TextUtils.equals(text, status.getText())) {
            status.setText(text);
        }
    }
}
