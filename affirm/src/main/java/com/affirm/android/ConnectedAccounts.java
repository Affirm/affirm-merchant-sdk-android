package com.affirm.android;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.affirm.android.AffirmTracker.TrackingEvent;
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
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_CHECKOUT_ARI;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_DECORATED;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_STATUS;
import static com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_VERIFIER_BYTES;
import static com.affirm.android.AffirmTracker.TrackingEvent.CONNECTED_ACCOUNTS_CHECKOUT_DECORATED;
import static com.affirm.android.AffirmTracker.TrackingEvent.CONNECTED_ACCOUNTS_PREPARE_AUTH;
import static com.affirm.android.AffirmTracker.TrackingLevel.INFO;

final class ConnectedAccounts {

    enum DecorateReason { DECORATED, MISSING_VERIFIER, ALREADY_CONSUMED, ERROR }

    interface TrackingSender {
        void send(@NonNull TrackingEvent event, @NonNull JsonObject data);
    }

    @VisibleForTesting
    static TrackingSender trackingSender =
            (event, data) -> AffirmTracker.track(event, INFO, data);

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Object LOCK = new Object();

    // Guarded by LOCK. Kept in memory only, never written to a Bundle.
    private static String codeVerifier;
    private static String codeChallenge;

    private ConnectedAccounts() {
    }

    static void prepareAuth(@NonNull Affirm.PrepareAuthCallback callback) {
        prepareAuth(callback, RANDOM);
    }

    @VisibleForTesting
    static void prepareAuth(@NonNull Affirm.PrepareAuthCallback callback,
                            @NonNull SecureRandom random) {
        if (AffirmPlugins.get() == null) {
            AffirmLog.w(AFFIRM_NOT_INITIALIZED_MESSAGE);
            callback.onPrepareAuthResult(AffirmPrepareAuthResult.create(Status.UNAVAILABLE, null));
            return;
        }

        final String challenge = cachedOrNewCodeChallenge(random);
        final AffirmPrepareAuthResult result = challenge != null
                ? AffirmPrepareAuthResult.create(Status.PENDING, challenge)
                : AffirmPrepareAuthResult.create(Status.UNAVAILABLE, null);
        trackingSender.send(CONNECTED_ACCOUNTS_PREPARE_AUTH,
                prepareAuthTrackingData(result.status()));
        callback.onPrepareAuthResult(result);
    }

    // Single use: a second call returns null until prepareAuth creates a new pair.
    @Nullable
    static String consumeCodeVerifier() {
        synchronized (LOCK) {
            final String verifier = codeVerifier;
            clear();
            return verifier;
        }
    }

    static void clear() {
        synchronized (LOCK) {
            codeVerifier = null;
            codeChallenge = null;
        }
    }

    // Returns redirectUrl unchanged unless both a session code and a code verifier are present.
    @NonNull
    static String decorateRedirectUrl(@NonNull String redirectUrl, @Nullable String sessionCode,
                                      @Nullable String codeVerifier) {
        if (TextUtils.isEmpty(sessionCode)) {
            return redirectUrl;
        }
        final Decoration decoration = decorate(redirectUrl, sessionCode, codeVerifier);
        trackDecoration(decoration.reason, redirectUrl);
        return decoration.url;
    }

    static void trackDecoration(@NonNull DecorateReason reason, @NonNull String redirectUrl) {
        trackingSender.send(CONNECTED_ACCOUNTS_CHECKOUT_DECORATED,
                decorateTrackingData(reason, redirectUrl));
    }

    @VisibleForTesting
    @NonNull
    static Decoration decorate(@NonNull String redirectUrl, @NonNull String sessionCode,
                               @Nullable String codeVerifier) {
        if (codeVerifier == null) {
            return new Decoration(redirectUrl, DecorateReason.MISSING_VERIFIER);
        }
        try {
            final Uri uri = Uri.parse(redirectUrl);
            // Uri.Builder drops query parameters on opaque URIs.
            if (!uri.isHierarchical()) {
                AffirmLog.w("Connected Accounts cannot decorate an opaque checkout URL");
                return new Decoration(redirectUrl, DecorateReason.ERROR);
            }
            final String decoratedUrl = uri.buildUpon()
                    .appendQueryParameter(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM, sessionCode)
                    .appendQueryParameter(CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM, codeVerifier)
                    .build()
                    .toString();
            return new Decoration(decoratedUrl, DecorateReason.DECORATED);
        } catch (RuntimeException e) {
            AffirmLog.w("Connected Accounts failed to decorate checkout URL", e);
            return new Decoration(redirectUrl, DecorateReason.ERROR);
        }
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
    static JsonObject decorateTrackingData(@NonNull DecorateReason reason,
                                           @NonNull String redirectUrl) {
        final JsonObject data = new JsonObject();
        data.addProperty(CONNECTED_ACCOUNTS_TRACK_DECORATED, reason == DecorateReason.DECORATED);
        // Same as the iOS SDK, which reads checkout_ari from the redirect URL's last path segment.
        data.addProperty(CONNECTED_ACCOUNTS_TRACK_CHECKOUT_ARI,
                Uri.parse(redirectUrl).getLastPathSegment());
        return data;
    }

    // Concurrent calls share one pair. Returns null if the pair can't be created.
    @Nullable
    private static String cachedOrNewCodeChallenge(@NonNull SecureRandom random) {
        synchronized (LOCK) {
            if (codeChallenge == null) {
                try {
                    final byte[] bytes = new byte[CONNECTED_ACCOUNTS_VERIFIER_BYTES];
                    random.nextBytes(bytes);
                    final String verifier = base64Url(bytes);
                    final MessageDigest digest =
                            MessageDigest.getInstance(CONNECTED_ACCOUNTS_DIGEST_ALGORITHM);
                    codeChallenge = base64Url(
                            digest.digest(verifier.getBytes(StandardCharsets.US_ASCII)));
                    codeVerifier = verifier;
                } catch (NoSuchAlgorithmException | RuntimeException e) {
                    AffirmLog.w("Connected Accounts prepareAuth failed", e);
                }
            }
            return codeChallenge;
        }
    }

    private static String base64Url(@NonNull byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    @VisibleForTesting
    static final class Decoration {
        @NonNull
        final String url;
        @NonNull
        final DecorateReason reason;

        Decoration(@NonNull String url, @NonNull DecorateReason reason) {
            this.url = url;
            this.reason = reason;
        }
    }
}
