package com.affirm.samples;

import android.view.View;

import androidx.annotation.NonNull;

import com.affirm.android.AffirmPromotionButton;
import com.affirm.android.PromotionWebView;

/** Tells from the views of an {@link AffirmPromotionButton} how its promo is shown. */
final class PromoViewState {

    private PromoViewState() {
    }

    @NonNull
    static String shownAs(@NonNull AffirmPromotionButton button) {
        if (button.getVisibility() != View.VISIBLE) {
            return "hidden";
        }
        for (int i = 0; i < button.getChildCount(); i++) {
            final View child = button.getChildAt(i);
            if (!(child instanceof PromotionWebView)) {
                return "Native";
            }
            if (child.getVisibility() == View.VISIBLE) {
                // The page stays transparent until it is ready.
                return child.getAlpha() > 0 ? "HTML" : "loading HTML…";
            }
        }
        return "loading…";
    }
}
