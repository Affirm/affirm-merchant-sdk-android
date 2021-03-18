package com.affirm.android;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.CookieManager;

import androidx.annotation.NonNull;

import com.affirm.android.exception.ConnectionException;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.Merchant;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static com.affirm.android.AffirmConstants.API_KEY;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;
import static com.affirm.android.AffirmConstants.INLINE_CHECKOUT;
import static com.affirm.android.AffirmConstants.INLINE_CHECKOUT_DIV;
import static com.affirm.android.AffirmConstants.INLINE_LEARN_MORE_CB_URL;
import static com.affirm.android.AffirmConstants.INLINE_LEARN_MORE_CLICK_URL;
import static com.affirm.android.AffirmConstants.JAVASCRIPT;
import static com.affirm.android.AffirmConstants.JS_PATH;
import static com.affirm.android.AffirmConstants.MERCHANT;
import static com.affirm.android.AffirmConstants.TEXT_HTML;
import static com.affirm.android.AffirmConstants.UTF_8;

public class InlineCheckoutWebView extends AffirmWebView implements AffirmWebChromeClient.Callbacks,
        AffirmWebViewClient.WebViewClientCallbacks, InlineWebViewClient.Callbacks {

    private OnClickListener onLearnMoreClickListener;

    public void setLearnMoreClickListener(OnClickListener onClickListener) {
        this.onLearnMoreClickListener = onClickListener;
    }

    public InlineCheckoutWebView(Context context) {
        this(context, null);
    }

    public InlineCheckoutWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
        } else {
            CookieManager.getInstance().setAcceptCookie(true);
        }
        AffirmUtils.debuggableWebView(getContext());
        setWebViewClient(new InlineWebViewClient(this));
        setWebChromeClient(new AffirmWebChromeClient(this));
    }

    void loadWebData(@NonNull final Checkout checkout) {
        final String html = initialHtml(checkout);
        loadDataWithBaseURL(
                HTTPS_PROTOCOL + AffirmPlugins.get().basePromoUrl(),
                html, TEXT_HTML, UTF_8, null);
    }

    private String initialHtml(@NonNull final Checkout checkout) {
        String html;
        try {
            final InputStream ins = getResources().openRawResource(R.raw.affirm_inline);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final HashMap<String, String> map = new HashMap<>();
        final String fullPath = HTTPS_PROTOCOL + AffirmPlugins.get().baseJsUrl() + JS_PATH;
        map.put(API_KEY, AffirmPlugins.get().publicKey());
        map.put(JAVASCRIPT, fullPath);

        final JsonObject checkoutJson = parseToJsonObject(checkout);
        Merchant merchant = Merchant.builder()
                .setPublicApiKey(AffirmPlugins.get().publicKey())
                .setName(AffirmPlugins.get().merchantName())
                .setInlineContainer(INLINE_CHECKOUT_DIV)
                .build();

        checkoutJson.add(MERCHANT, parseToJsonObject(merchant));

        map.put(INLINE_CHECKOUT, checkoutJson.toString());
        map.put(INLINE_LEARN_MORE_CB_URL, INLINE_LEARN_MORE_CLICK_URL);

        return AffirmUtils.replacePlaceholders(html, map);
    }

    private JsonObject parseToJsonObject(Object object) {
        final JsonParser jsonParser = new JsonParser();
        return jsonParser.parse(AffirmPlugins.get().gson().toJson(object)).getAsJsonObject();
    }

    @Override
    public void chromeLoadCompleted() {
        AffirmLog.v("InlineWebView has been loaded");
    }

    @Override
    public void onWebViewError(@NonNull ConnectionException error) {
        AffirmLog.e("InlineWebView load failed" + error.toString());
    }

    @Override
    public void onLearnMoreClicked() {
        AffirmLog.v("InlineWebView learn more clicked");
        if (onLearnMoreClickListener != null) {
            onLearnMoreClickListener.onClick(this);
        }
    }
}
