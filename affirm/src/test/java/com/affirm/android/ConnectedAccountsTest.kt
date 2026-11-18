package com.affirm.android

import android.net.Uri
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_SESSION_CODE_PARAM
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_CHECKOUT_ARI
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_DECORATED
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_STATUS
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_VERIFIER_BYTES
import com.affirm.android.ConnectedAccounts.DecorateReason
import com.affirm.android.model.AffirmPrepareAuthResult
import com.affirm.android.model.AffirmPrepareAuthResult.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.SecureRandom
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ConnectedAccountsTest {

    @Before
    fun setup() {
        if (AffirmPlugins.get() == null) {
            Affirm.initialize(
                Affirm.Configuration.Builder("Y8CQXFF044903JC0", Affirm.Environment.SANDBOX)
                    .build()
            )
        }
        ConnectedAccounts.clear()
    }

    @Test
    fun returnsPendingWithChallenge() {
        val result = prepareAuth()
        assertEquals(Status.PENDING, result.status)
        assertNotNull(result.codeChallenge)
    }

    @Test
    fun challengeIsS256OfVerifier() {
        val challenge = prepareAuth().codeChallenge!!
        val verifier = ConnectedAccounts.consumeCodeVerifier()!!

        assertEquals(s256(verifier), challenge)
        // base64url, no padding
        assertEquals((CONNECTED_ACCOUNTS_VERIFIER_BYTES * 4 + 2) / 3, verifier.length)
        assertTrue(verifier.matches(Regex("[A-Za-z0-9_-]+")))
    }

    @Test
    fun matchesRfc7636Example() {
        // RFC 7636 Appendix B
        val octets = intArrayOf(
            116, 24, 223, 180, 151, 153, 224, 37, 79, 250, 96, 125, 216, 173, 187, 186,
            22, 212, 37, 77, 105, 214, 191, 240, 91, 88, 5, 88, 83, 132, 141, 121
        )
        val rfcRandom = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                octets.forEachIndexed { i, octet -> bytes[i] = octet.toByte() }
            }
        }
        var result: AffirmPrepareAuthResult? = null
        ConnectedAccounts.prepareAuth(Affirm.PrepareAuthCallback { result = it }, rfcRandom)

        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", result!!.codeChallenge)
        assertEquals("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
            ConnectedAccounts.consumeCodeVerifier())
    }

    @Test
    fun repeatedCallsReturnSameChallengeUntilConsumed() {
        val first = prepareAuth().codeChallenge
        assertEquals(first, prepareAuth().codeChallenge)

        assertNotNull(ConnectedAccounts.consumeCodeVerifier())
        assertNotEquals(first, prepareAuth().codeChallenge)
    }

    @Test
    fun consumeIsSingleUse() {
        prepareAuth()
        assertNotNull(ConnectedAccounts.consumeCodeVerifier())
        assertNull(ConnectedAccounts.consumeCodeVerifier())
    }

    @Test
    fun randomFailureReturnsUnavailable() {
        val failingRandom = object : SecureRandom() {
            override fun nextBytes(bytes: ByteArray) {
                throw IllegalStateException("no entropy")
            }
        }
        var result: AffirmPrepareAuthResult? = null
        ConnectedAccounts.prepareAuth(Affirm.PrepareAuthCallback { result = it }, failingRandom)

        assertEquals(Status.UNAVAILABLE, result!!.status)
        assertNull(result!!.codeChallenge)
        assertNull(ConnectedAccounts.consumeCodeVerifier())
    }

    @Test
    fun notInitializedReturnsUnavailable() {
        val configuration = AffirmPlugins.get().configuration
        AffirmPlugins.reset()
        try {
            var result: AffirmPrepareAuthResult? = null
            ConnectedAccounts.prepareAuth { result = it }
            assertEquals(Status.UNAVAILABLE, result!!.status)
            assertNull(result!!.codeChallenge)
            assertNull(ConnectedAccounts.consumeCodeVerifier())
        } finally {
            Affirm.initialize(configuration)
        }
    }

    @Test
    fun concurrentCallsShareOnePairAndConsumeSucceedsOnce() {
        val threads = 16
        val pool = Executors.newFixedThreadPool(threads)
        try {
            val challenges = Collections.synchronizedSet(HashSet<String?>())
            runConcurrently(pool, threads) { challenges.add(prepareAuth().codeChallenge) }
            assertEquals(1, challenges.size)
            assertNotNull(challenges.first())

            val verifiers = Collections.synchronizedList(ArrayList<String>())
            runConcurrently(pool, threads) {
                ConnectedAccounts.consumeCodeVerifier()?.let { verifiers.add(it) }
            }
            assertEquals(1, verifiers.size)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun decorateWithoutSessionCodeLeavesUrl() {
        assertEquals(TEST_REDIRECT_URL, ConnectedAccounts.decorateRedirectUrl(TEST_REDIRECT_URL, null, "v_1"))
    }

    @Test
    fun decorateWithoutVerifierLeavesUrl() {
        assertEquals(TEST_REDIRECT_URL, ConnectedAccounts.decorateRedirectUrl(TEST_REDIRECT_URL, "sc_1", null))
    }

    @Test
    fun decorateAppendsSessionCodeAndVerifier() {
        val uri = Uri.parse(ConnectedAccounts.decorateRedirectUrl(TEST_REDIRECT_URL, "sc_1", "v_1"))
        assertEquals("abc", uri.getQueryParameter("token"))
        assertEquals("sc_1", uri.getQueryParameter(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM))
        assertEquals("v_1", uri.getQueryParameter(CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM))
    }

    @Test
    fun decorateKeepsUrlFragmentAfterQuery() {
        val uri = Uri.parse(
            ConnectedAccounts.decorateRedirectUrl("$TEST_REDIRECT_URL#step", "sc_1", "v_1")
        )
        assertEquals("step", uri.fragment)
        assertEquals("abc", uri.getQueryParameter("token"))
        assertEquals("sc_1", uri.getQueryParameter(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM))
        assertEquals("v_1", uri.getQueryParameter(CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM))
    }

    @Test
    fun decorateAppendsToUrlWithoutQuery() {
        val uri = Uri.parse(
            ConnectedAccounts.decorateRedirectUrl("https://sandbox.affirm.com/checkout/", "sc_1", "v_1")
        )
        assertEquals("/checkout/", uri.path)
        assertEquals(
            setOf(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM, CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM),
            uri.queryParameterNames
        )
        assertEquals("sc_1", uri.getQueryParameter(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM))
        assertEquals("v_1", uri.getQueryParameter(CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM))
    }

    @Test
    fun decorateReportsDecorated() {
        val decoration = ConnectedAccounts.decorate(TEST_REDIRECT_URL, "sc_1", "v_1")
        assertEquals(DecorateReason.DECORATED, decoration.reason)
        assertNotEquals(TEST_REDIRECT_URL, decoration.url)
    }

    @Test
    fun decorateReportsMissingVerifier() {
        val decoration = ConnectedAccounts.decorate(TEST_REDIRECT_URL, "sc_1", null)
        assertEquals(DecorateReason.MISSING_VERIFIER, decoration.reason)
        assertEquals(TEST_REDIRECT_URL, decoration.url)
    }

    @Test
    fun decorateOpaqueUrlReportsErrorAndLeavesUrl() {
        val opaqueUrl = "mailto:checkout@affirm.com"
        val decoration = ConnectedAccounts.decorate(opaqueUrl, "sc_1", "v_1")
        assertEquals(DecorateReason.ERROR, decoration.reason)
        assertEquals(opaqueUrl, decoration.url)
    }

    @Test
    fun decorateDoesNotConsumeVerifier() {
        prepareAuth()
        ConnectedAccounts.decorateRedirectUrl(TEST_REDIRECT_URL, "sc_1", "v_1")
        assertNotNull(ConnectedAccounts.consumeCodeVerifier())
    }

    @Test
    fun consumeAfterConsumedVerifierNeedsFreshPair() {
        val firstChallenge = prepareAuth().codeChallenge
        val first = ConnectedAccounts.consumeCodeVerifier()!!
        assertNull(ConnectedAccounts.consumeCodeVerifier())

        val secondChallenge = prepareAuth().codeChallenge!!
        val second = ConnectedAccounts.consumeCodeVerifier()!!

        assertNotEquals(firstChallenge, secondChallenge)
        assertNotEquals(first, second)
        assertEquals(secondChallenge, s256(second))
    }

    @Test
    fun prepareAuthTrackingDataReportsStatus() {
        assertEquals("pending",
            ConnectedAccounts.prepareAuthTrackingData(Status.PENDING).get(CONNECTED_ACCOUNTS_TRACK_STATUS).asString)
        assertEquals("unavailable",
            ConnectedAccounts.prepareAuthTrackingData(Status.UNAVAILABLE).get(CONNECTED_ACCOUNTS_TRACK_STATUS).asString)
    }

    @Test
    fun decorateTrackingDataMatchesIos() {
        assertTrackingData(DecorateReason.DECORATED, true)
        assertTrackingData(DecorateReason.MISSING_VERIFIER, false)
        assertTrackingData(DecorateReason.ERROR, false)
    }

    @Test
    fun decorateTrackingDataWithoutPathHasNullCheckoutAri() {
        val data = ConnectedAccounts.decorateTrackingData(
            DecorateReason.ERROR, "mailto:checkout@affirm.com"
        )
        assertTrue(data.get(CONNECTED_ACCOUNTS_TRACK_CHECKOUT_ARI).isJsonNull)
    }

    @Test
    fun trackingFailureDoesNotThrow() {
        // Tracking reads AffirmPlugins synchronously, so it fails while the SDK is not initialized.
        withoutAffirmPlugins {
            AffirmTracker.track(
                AffirmTracker.TrackingEvent.CONNECTED_ACCOUNTS_PREPARE_AUTH,
                AffirmTracker.TrackingLevel.INFO,
                ConnectedAccounts.prepareAuthTrackingData(Status.PENDING)
            )
        }
    }

    @Test
    fun decorateReturnsUrlWhenTrackingFails() {
        withoutAffirmPlugins {
            val uri = Uri.parse(ConnectedAccounts.decorateRedirectUrl(TEST_REDIRECT_URL, "sc_1", "v_1"))
            assertEquals("sc_1", uri.getQueryParameter(CONNECTED_ACCOUNTS_SESSION_CODE_PARAM))
            assertEquals("v_1", uri.getQueryParameter(CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM))

            assertEquals(TEST_REDIRECT_URL,
                ConnectedAccounts.decorateRedirectUrl(TEST_REDIRECT_URL, "sc_1", null))
        }
    }

    private fun withoutAffirmPlugins(block: () -> Unit) {
        val configuration = AffirmPlugins.get().configuration
        AffirmPlugins.reset()
        try {
            block()
        } finally {
            Affirm.initialize(configuration)
        }
    }

    private fun assertTrackingData(reason: DecorateReason, decorated: Boolean) {
        val data = ConnectedAccounts.decorateTrackingData(
            reason, "https://sandbox.affirm.com/checkout/ARI123?token=abc"
        )
        assertEquals(setOf(CONNECTED_ACCOUNTS_TRACK_DECORATED, CONNECTED_ACCOUNTS_TRACK_CHECKOUT_ARI), data.keySet())
        assertEquals(decorated, data.get(CONNECTED_ACCOUNTS_TRACK_DECORATED).asBoolean)
        assertEquals("ARI123", data.get(CONNECTED_ACCOUNTS_TRACK_CHECKOUT_ARI).asString)
    }

    private fun prepareAuth(): AffirmPrepareAuthResult {
        var result: AffirmPrepareAuthResult? = null
        ConnectedAccounts.prepareAuth { result = it }
        return result!!
    }

    private fun runConcurrently(
        pool: ExecutorService,
        count: Int,
        task: () -> Unit
    ) {
        val start = CountDownLatch(1)
        val done = CountDownLatch(count)
        repeat(count) {
            pool.execute {
                start.await()
                try {
                    task()
                } finally {
                    done.countDown()
                }
            }
        }
        start.countDown()
        assertTrue(done.await(10, TimeUnit.SECONDS))
    }
}
