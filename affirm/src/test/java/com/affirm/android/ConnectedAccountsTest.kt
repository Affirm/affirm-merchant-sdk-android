package com.affirm.android

import android.net.Uri
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_CODE_VERIFIER_PARAM
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_SESSION_CODE_PARAM
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_DECORATED
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_TRACK_STATUS
import com.affirm.android.AffirmConstants.CONNECTED_ACCOUNTS_VERIFIER_BYTES
import com.affirm.android.model.AffirmPrepareAuthResult
import com.affirm.android.model.AffirmPrepareAuthResult.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        AffirmPlugins.get().consumeCodeVerifier()
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
        val verifier = AffirmPlugins.get().consumeCodeVerifier()!!

        assertEquals(s256(verifier), challenge)
        // base64url, no padding
        assertEquals((CONNECTED_ACCOUNTS_VERIFIER_BYTES * 4 + 2) / 3, verifier.length)
        assertTrue(verifier.matches(Regex("[A-Za-z0-9_-]+")))
    }

    @Test
    fun repeatedCallsReturnSameChallengeUntilConsumed() {
        val first = prepareAuth().codeChallenge
        assertEquals(first, prepareAuth().codeChallenge)

        assertNotNull(AffirmPlugins.get().consumeCodeVerifier())
        assertNotEquals(first, prepareAuth().codeChallenge)
    }

    @Test
    fun consumeIsSingleUse() {
        prepareAuth()
        assertNotNull(AffirmPlugins.get().consumeCodeVerifier())
        assertNull(AffirmPlugins.get().consumeCodeVerifier())
        assertNull(AffirmPlugins.get().cachedCodeChallenge())
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
        assertNull(AffirmPlugins.get().consumeCodeVerifier())
    }

    @Test
    fun notInitializedReturnsUnavailable() {
        val configuration = AffirmPlugins.get().configuration
        AffirmPlugins.reset()
        try {
            var result: AffirmPrepareAuthResult? = null
            Affirm.prepareAuth { result = it }
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
                AffirmPlugins.get().consumeCodeVerifier()?.let { verifiers.add(it) }
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
    fun decorateTrackingDataReportsDecorated() {
        assertTrue(ConnectedAccounts.decorateTrackingData(true).get(CONNECTED_ACCOUNTS_TRACK_DECORATED).asBoolean)
        assertFalse(ConnectedAccounts.decorateTrackingData(false).get(CONNECTED_ACCOUNTS_TRACK_DECORATED).asBoolean)
    }

    private fun prepareAuth(): AffirmPrepareAuthResult {
        var result: AffirmPrepareAuthResult? = null
        Affirm.prepareAuth { result = it }
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
