package com.affirm.samples;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Answers the way a merchant server would, without a network. Affirm rejects its session codes,
 * so a checkout started with one continues as a normal sign-in.
 */
final class MockMerchantBackend implements MerchantBackend {

    enum Mode { LINKED, NOT_LINKED, TOKEN_REJECTED }

    private static final long LATENCY_MS = 500;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @NonNull
    private Mode mode = Mode.LINKED;

    void setMode(@NonNull Mode mode) {
        this.mode = mode;
    }

    @Override
    public void requestSessionCode(@NonNull String accountLinkingId,
                                   @NonNull String codeChallenge,
                                   @NonNull Callback callback) {
        final Mode answering = mode;
        handler.postDelayed(() -> callback.onResult(sessionCode(answering, codeChallenge),
                detail(answering, accountLinkingId)), LATENCY_MS);
    }

    @Override
    public void cancel() {
        handler.removeCallbacksAndMessages(null);
    }

    @Nullable
    private static String sessionCode(@NonNull Mode mode, @NonNull String codeChallenge) {
        return mode == Mode.LINKED ? "mock_session_" + codeChallenge.substring(0, 8) : null;
    }

    @NonNull
    private static String detail(@NonNull Mode mode, @NonNull String accountLinkingId) {
        switch (mode) {
            case LINKED:
                return "refresh token found for " + accountLinkingId
                        + ", session code issued (mock, Affirm will reject it)";
            case NOT_LINKED:
                return "no refresh token for " + accountLinkingId
                        + ", the customer has not opted in";
            default:
                return "token exchange returned 401 for " + accountLinkingId
                        + ", delete the stored token and check out normally";
        }
    }
}
