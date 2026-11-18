package com.affirm.android

import android.util.Base64
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_DIGEST_ALGORITHM
import java.security.MessageDigest

internal const val TEST_REDIRECT_URL = "https://sandbox.affirm.com/checkout/?token=abc"

/** The PKCE S256 challenge for [verifier]. */
internal fun s256(verifier: String): String = Base64.encodeToString(
    MessageDigest.getInstance(CONNECTED_ACCOUNTS_DIGEST_ALGORITHM)
        .digest(verifier.toByteArray(Charsets.US_ASCII)),
    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
)
