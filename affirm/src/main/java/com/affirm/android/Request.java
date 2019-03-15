package com.affirm.android;

import android.os.AsyncTask;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class Request {

    static boolean isRequestCancelled = false;

    void executeTask(@Nullable Executor executor,
                     @NonNull AsyncTask<Void, Void, ResponseWrapper> task) {
        if (executor != null) {
            task.executeOnExecutor(executor);
        } else {
            task.execute();
        }
    }
}
