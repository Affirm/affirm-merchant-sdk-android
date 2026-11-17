package com.affirm.android;

import android.net.Uri;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.model.AffirmPrepareAuthResult;
import com.affirm.android.model.AffirmPrepareAuthResult.Status;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

import static com.affirm.android.AffirmConstants.AFFIRM_NOT_INITIALIZED_MESSAGE;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_DIGEST_ALGORITHM;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_SESSION_CODE_PARAM;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_DECORATED;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_STATUS;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_VERIFIER_BYTES;
import static com.affirm.android.AffirmTracker.TrackingEvent.CONNECTED_ACCOUNTS_CHECKOUT_DECORATED;
import static com.affirm.android.AffirmTracker.TrackingEvent.CONNECTED_ACCOUNTS_PREPARE_AUTH;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;

// Package-private so merchant code can never reach the code verifier.
final class ConnectedAccounts {

    private ConnectedAccounts() {
    }

    static void prepareAuth(@NonNull Affirm.PrepareAuthCallback callback) {
        prepareAuth(callback, new SecureRandom());
    }

    @VisibleForTesting
    static void prepareAuth(@NonNull Affirm.PrepareAuthCallback callback,
                            @NonNull SecureRandom random) {
        final AffirmPlugins plugins = AffirmPlugins.get();
        if (plugins == null) {
            AffirmLog.w(AFFIRM_NOT_INITIALIZED_MESSAGE);
            callback.onPrepareAuthResult(AffirmPrepareAuthResult.create(Status.UNAVAILABLE, null));
            return;
        }

        String challenge;
        // One lock across lookup and generate, so concurrent calls share a pair.
        synchronized (plugins) {
            challenge = plugins.cachedCodeChallenge();
            if (challenge == null) {
                try {
                    final byte[] bytes = new byte[CONNECTED_ACCOUNTS_VERIFIER_BYTES];
                    random.nextBytes(bytes);
                    final String verifier = base64Url(bytes);
                    final MessageDigest digest =
                            MessageDigest.getInstance(CONNECTED_ACCOUNTS_DIGEST_ALGORITHM);
                    challenge = base64Url(
                            digest.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                    plugins.cachePrepareAuthState(verifier, challenge);
                } catch (NoSuchAlgorithmException | RuntimeException e) {
                    AffirmLog.w("Connected Accounts prepareAuth failed", e);
                    challenge = null;
                }
            }
        }

        final AffirmPrepareAuthResult result = challenge != null
                ? AffirmPrepareAuthResult.create(Status.PENDING, challenge)
                : AffirmPrepareAuthResult.create(Status.UNAVAILABLE, null);
        AffirmTracker.track(CONNECTED_ACCOUNTS_PREPARE_AUTH, INFO,
                prepareAuthTrackingData(result.status()));
        callback.onPrepareAuthResult(result);
    }

    // Single use: a second call returns null until prepareAuth creates a new pair.
    @Nullable
    static String consumeCodeVerifier() {
        final AffirmPlugins plugins = AffirmPlugins.get();
        return plugins != null ? plugins.consumeCodeVerifier() : null;
    }

    // Returns redirectUrl unchanged unless both a session code and a code verifier are present.
    @NonNull
    static String decorateRedirectUrl(@NonNull String redirectUrl, @Nullable String sessionCode,
                                      @Nullable String codeVerifier) {
        if (sessionCode == null) {
            return redirectUrl;
        }
        String decoratedUrl = redirectUrl;
        boolean decorated = false;
        if (codeVerifier != null) {
            try {
                decoratedUrl = Uri.parse(redirectUrl).buildUpon()
                        .appendQueryParameter(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM, sessionCode)
                        .appendQueryParameter(CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM, codeVerifier)
                        .build()
                        .toString();
                decorated = true;
            } catch (RuntimeException e) {
                AffirmLog.w("Connected Accounts failed to decorate checkout URL", e);
            }
        }
        AffirmTracker.track(CONNECTED_ACCOUNTS_CHECKOUT_DECORATED, INFO,
                decorateTrackingData(decorated));
        return decoratedUrl;
    }

    @VisibleForTesting
    @NonNull
    static JsonObject prepareAuthTrackingData(@NonNull Status status) {
        final JsonObject data = new JsonObject();
        data.addProperty(CONNECTED_ACCOUNTS_TRACK_STATUS, status.name().toLowerCase(Locale.US));
        return data;
    }

    @VisibleForTesting
    @NonNull
    static JsonObject decorateTrackingData(boolean decorated) {
        final JsonObject data = new JsonObject();
        data.addProperty(CONNECTED_ACCOUNTS_TRACK_DECORATED, decorated);
        return data;
    }

    private static String base64Url(@NonNull byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
