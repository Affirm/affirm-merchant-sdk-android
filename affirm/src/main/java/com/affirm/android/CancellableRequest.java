package com.affirm.android;

interface CancellableRequest {
    void cancelRequest();

    void executeRequest();
}
