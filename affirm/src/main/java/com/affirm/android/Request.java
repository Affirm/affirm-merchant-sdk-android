package com.affirm.android;

import android.os.AsyncTask;

abstract class Request<T> {

    interface RequestCreate {

        void create();

        void cancel();
    }

    abstract void create();

    abstract void cancel();

    abstract AsyncTask<Void, Void, T> createTask();

    abstract void cancelTask();

    private AsyncTask<Void, Void, T> task;

    static boolean isRequestCancelled = false;

    final RequestCreate requestCreate = new RequestCreate() {

        @Override
        public void create() {
            isRequestCancelled = false;
            task = createTask();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        @Override
        public void cancel() {
            if (task != null && !task.isCancelled()) {
                task.cancel(true);
                task = null;
            }
            isRequestCancelled = true;
            cancelTask();
        }
    };
}
