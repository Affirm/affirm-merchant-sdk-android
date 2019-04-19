package com.affirm.android;

import android.os.AsyncTask;

abstract class AffirmRequest<T> {

    interface RequestCreate {

        void create();

        void cancel();
    }

    abstract AsyncTask<Void, Void, T> createTask();

    private AsyncTask<Void, Void, T> task;

    static boolean isRequestCancelled = false;

    private final RequestCreate mRequestCreate = new RequestCreate() {

        @Override
        public void create() {
            setRequestCancelled(false);
            task = createTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void cancel() {
            if (task != null && !task.isCancelled()) {
                task.cancel(true);
                task = null;
            }
            setRequestCancelled(true);
        }

    };

    public static void setRequestCancelled(boolean value) {
        isRequestCancelled = value;
    }

    void create() {
        mRequestCreate.create();
    }

    void cancel() {
        mRequestCreate.cancel();
    }
}
