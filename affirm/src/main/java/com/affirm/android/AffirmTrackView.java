package com.affirm.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.FrameLayout;

import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.AffirmTrack;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import androidx.annotation.NonNull;

import static com.affirm.android.AffirmConstants.API_KEY;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.JAVASCRIPT;
import static com.affirm.android.AffirmConstants.JS_PATH;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.TRACK_ORDER_OBJECT;
import static com.affirm.android.AffirmConstants.TRACK_PRODUCT_OBJECT;
import static com.affirm.android.AffirmConstants.UTF_8;

@SuppressLint("ViewConstructor")
public class AffirmTrackView extends FrameLayout
        implements AffirmWebViewClient.WebViewClientCallbacks, AffirmWebChromeClient.Callbacks {

    interface AffirmTrackCallback {

        void onSuccess(AffirmTrackView affirmTrackView);

        void onFailed(AffirmTrackView affirmTrackView, String reason);
    }

    private AffirmWebView webView;

    private AffirmTrack affirmTrack;
    private AffirmTrackCallback affirmTrackCallback;

    private static final int DELAY_FINISH = 10 * 1000;
    private Handler handler = new Handler();

    public AffirmTrackView(@NonNull Context context, @NonNull AffirmTrack affirmTrack,
                           @NonNull AffirmTrackCallback affirmTrackCallback) {
        super(context, null, 0);

        this.affirmTrack = affirmTrack;
        this.affirmTrackCallback = affirmTrackCallback;

        setVisibility(View.GONE);

        initViews(context);
    }

    private void initViews(Context context) {
        addView(webView = new AffirmWebView(context.getApplicationContext(), null));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        AffirmLog.v("AffirmTrackView attached to window");

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }
        AffirmUtils.debuggableWebView(getContext());
        webView.setWebViewClient(new TrackWebViewClient(this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));

        final String html = initialHtml();
        webView.loadDataWithBaseURL(HTTPS_PROTOCOL + AffirmPlugins.get().baseUrl(), html,
                TEXT_HTML, UTF_8, null);
        // Since there is no callback, the track view will be removed after 10 seconds timeout.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                affirmTrackCallback.onSuccess(AffirmTrackView.this);
            }
        }, DELAY_FINISH);
    }

    private String initialHtml() {
        String html;
        try {
            final InputStream ins =
                    getResources().openRawResource(R.raw.affirm_track_order_confirmed);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final String fullPath = HTTPS_PROTOCOL + AffirmPlugins.get().baseJsUrl() + JS_PATH;

        final HashMap<String, String> map = new HashMap<>();

        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);
        map.put(TRACK_ORDER_OBJECT, buildOrderObject().toString());
        map.put(TRACK_PRODUCT_OBJECT, buildProductObject().toString());
        return AffirmUtils.replacePlaceholders(html, map);
    }

    private JsonObject buildOrderObject() {
        final JsonParser jsonParser = new JsonParser();
        return jsonParser.parse(AffirmPlugins.get().gson()
                .toJson(affirmTrack.affirmTrackOrder())).getAsJsonObject();
    }

    private JsonArray buildProductObject() {
        final JsonParser jsonParser = new JsonParser();
        return jsonParser.parse(AffirmPlugins.get().gson()
                .toJson(affirmTrack.affirmTrackProducts())).getAsJsonArray();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        AffirmLog.v("AffirmTrackView detached from window");
        Context context = getContext();
        if (context != null) {
            ViewGroup container =
                    ((Activity) context).getWindow()
                            .getDecorView().findViewById(android.R.id.content);
            container.removeView(webView);
        }
        webView.removeAllViews();
        webView.destroyDrawingCache();
        webView.clearHistory();
        webView.destroy();
        webView = null;
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        affirmTrackCallback.onFailed(this, error.toString());
    }

    @Override
    public void chromeLoadCompleted() {
        AffirmLog.v("AffirmTrackView has been loaded");
    }
}
