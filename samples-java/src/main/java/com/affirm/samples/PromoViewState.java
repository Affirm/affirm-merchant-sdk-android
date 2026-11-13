package com.affirm.samples;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
                return "native text";
            }
            if (child.getVisibility() == View.VISIBLE) {
                // The page stays transparent until it is ready.
                return child.getAlpha() > 0 ? "HTML" : "loading HTML…";
            }
        }
        return "loading…";
    }

    /** What TalkBack reads for {@code view}: its own description, or its first child's. */
    @Nullable
    static CharSequence talkBackLabel(@NonNull View view) {
        if (view.getContentDescription() != null || !(view instanceof ViewGroup)) {
            return view.getContentDescription();
        }
        final ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            final CharSequence label = talkBackLabel(group.getChildAt(i));
            if (label != null) {
                return label;
            }
        }
        return null;
    }
}
