package com.affirm.android;

interface CancellableRequest {
    void cancel();

    void execute();
}
