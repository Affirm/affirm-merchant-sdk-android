package com.affirm.android;

import com.affirm.android.exception.AffirmException;

class AffirmResponseWrapper<T> {
    T source;

    AffirmException error;

    AffirmResponseWrapper(T source) {
        this.source = source;
    }

    AffirmResponseWrapper(AffirmException error) {
        this.error = error;
    }
}
