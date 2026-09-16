package com.affirm.android;

import androidx.annotation.Nullable;

import com.affirm.android.exception.AffirmException;
import com.affirm.android.model.AffirmError;
import com.affirm.android.model.AffirmErrorUi;
import com.google.common.base.Strings;

final class CheckoutErrors {

    private CheckoutErrors() {
    }

    @Nullable
    static String messageFrom(@Nullable AffirmException exception) {
        if (exception == null) {
            return null;
        }

        if (!Strings.isNullOrEmpty(exception.getMessage())) {
            return exception.getMessage();
        }

        final AffirmError affirmError = affirmErrorFrom(exception);
        if (affirmError != null) {
            if (!Strings.isNullOrEmpty(affirmError.message())) {
                return affirmError.message();
            }

            final AffirmErrorUi ui = affirmError.ui();
            if (ui != null && !Strings.isNullOrEmpty(ui.main())) {
                return ui.main();
            }
        }

        return exception.toString();
    }

    @Nullable
    static AffirmError affirmErrorFrom(@Nullable AffirmException exception) {
        if (exception == null) {
            return null;
        }

        if (exception.getAffirmError() != null) {
            return exception.getAffirmError();
        }

        final Throwable cause = exception.getCause();
        return cause instanceof AffirmException
                ? ((AffirmException) cause).getAffirmError()
                : null;
    }
}
