package com.affirm.android;

import com.affirm.android.exception.AffirmException;

class ResponseWrapper<T> {
    T source;

    AffirmException error;

    ResponseWrapper(T source) {
        this.source = source;
    }

    ResponseWrapper(AffirmException error) {
        this.error = error;
    }
}
