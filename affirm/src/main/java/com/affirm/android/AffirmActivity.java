package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

abstract class AffirmActivity extends AppCompatActivity implements AffirmWebChromeClient.Callbacks {

    AffirmWebView webView;
    View progressIndicator;

    static void startForResult(@NonNull Activity originalActivity,
                               @NonNull Intent intent,
                               int requestCode) {
        originalActivity.startActivityForResult(intent, requestCode);
    }


    static void startForResult(@NonNull Fragment originalFragment,
                               @NonNull Intent intent,
                               int requestCode) {
        originalFragment.startActivityForResult(intent, requestCode);
    }

    abstract void initViews();

    abstract void beforeOnCreate();

    abstract void initData(@Nullable Bundle savedInstanceState);

    abstract void onAttached();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        beforeOnCreate();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.affirm_activity_webview);
        webView = findViewById(R.id.webview);
        progressIndicator = findViewById(R.id.progressIndicator);

        initViews();

        initData(savedInstanceState);

        onAttached();
    }

    @Override
    protected void onDestroy() {
        webView.destroyWebView();
        webView = null;
        super.onDestroy();
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }
}
