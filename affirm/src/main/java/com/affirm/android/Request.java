package com.affirm.android;

import android.os.AsyncTask;

abstract class Request<T> {

    interface RequestCreate {

        void create();

        void cancel();
    }

    abstract AsyncTask<Void, Void, T> createTask();

    private AsyncTask<Void, Void, T> mTask;

    static boolean isRequestCancelled = false;

    private final RequestCreate mRequestCreate = new RequestCreate() {

        @Override
        public void create() {
            isRequestCancelled = false;
            mTask = createTask();
            mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void cancel() {
            if (mTask != null && !mTask.isCancelled()) {
                mTask.cancel(true);
                mTask = null;
            }
            isRequestCancelled = true;
        }
    };

    void create() {
        mRequestCreate.create();
    }

    void cancel() {
        mRequestCreate.cancel();
    }
}
