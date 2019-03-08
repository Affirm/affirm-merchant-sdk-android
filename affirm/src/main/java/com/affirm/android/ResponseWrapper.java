package com.affirm.android;

class ResponseWrapper<T> {
    T source;

    Exception error;

    ResponseWrapper(T source) {
        this.source = source;
    }

    ResponseWrapper(Exception error) {
        this.error = error;
    }
}
