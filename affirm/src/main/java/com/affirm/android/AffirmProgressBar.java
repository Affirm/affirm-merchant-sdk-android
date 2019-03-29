package com.affirm.android;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ProgressBar;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

class AffirmProgressBar extends ProgressBar {

    public AffirmProgressBar(Context context) {
        this(context, null);
    }

    public AffirmProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AffirmProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Drawable drawableProgress = DrawableCompat.wrap(getIndeterminateDrawable());
            DrawableCompat.setTint(drawableProgress,
                    ContextCompat.getColor(getContext(), R.color.affirm_indigo));
            setIndeterminateDrawable(DrawableCompat.unwrap(drawableProgress));
        } else {
            getIndeterminateDrawable().setColorFilter(
                    ContextCompat.getColor(getContext(), R.color.affirm_indigo),
                    PorterDuff.Mode.SRC_IN);
        }
    }
}