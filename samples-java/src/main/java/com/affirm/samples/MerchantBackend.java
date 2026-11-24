package com.affirm.samples;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The merchant server's part of Connected Accounts. It keeps the customer's refresh token and the
 * private key, so the app never calls Affirm's OAuth endpoints itself.
 */
interface MerchantBackend {

    interface Callback {
        /** {@code sessionCode} is null when the customer can't be accelerated. */
        void onResult(@Nullable String sessionCode, @NonNull String detail);
    }

    /**
     * Exchanges the stored refresh token for an access token (POST /api/pba/v1/oauth/token), then
     * the access token and {@code codeChallenge} for a session code
     * (POST /api/pba/v1/oauth/session). Calls back on the main thread.
     */
    void requestSessionCode(@NonNull String accountLinkingId, @NonNull String codeChallenge,
                            @NonNull Callback callback);

    /** Drops the pending request; its callback is not called. */
    void cancel();
}
