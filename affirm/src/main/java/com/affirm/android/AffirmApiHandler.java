package com.affirm.android;

import static com.affirm.android.AffirmConstants.HTTP;
import static com.affirm.android.AffirmConstants.HTTPS_PROTOCOL;

final class AffirmApiHandler {
    private AffirmApiHandler() {
    }

    static String getProtocol() {
        return AffirmPlugins.get().baseUrl().contains(HTTP) ? "" : HTTPS_PROTOCOL;
    }
}
