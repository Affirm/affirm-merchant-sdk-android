package com.affirm.android;

import android.os.AsyncTask;

abstract class AffirmRequest<T> {

    interface RequestCreate {

        void create();

        void cancel();
    }

    abstract T createTask();

    void cancelTask() {
    }

    T task;

    private final RequestCreate requestCreate = new RequestCreate() {

        @Override
        public void create() {
            task = createTask();

            if (task instanceof AsyncTask) {
                ((AsyncTask) task).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }

        @Override
        public void cancel() {
            if (task != null) {
                if (task instanceof AsyncTask && !((AsyncTask) task).isCancelled()) {
                    ((AsyncTask) task).cancel(true);
                }

                task = null;
            }

            cancelTask();
        }
    };

    void create() {
        requestCreate.create();
    }

    void cancel() {
        requestCreate.cancel();
    }
}
