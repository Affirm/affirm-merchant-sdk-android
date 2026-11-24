package com.affirm.android.model

import com.affirm.android.model.AffirmPrepareAuthResult.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AffirmPrepareAuthResultTest {

    @Test
    fun createPendingWithChallenge() {
        val result = AffirmPrepareAuthResult.create(Status.PENDING, "challenge")
        assertEquals(Status.PENDING, result.status)
        assertEquals("challenge", result.codeChallenge)
    }

    @Test
    fun createUnavailableWithoutChallenge() {
        val result = AffirmPrepareAuthResult.create(Status.UNAVAILABLE, null)
        assertEquals(Status.UNAVAILABLE, result.status)
        assertNull(result.codeChallenge)
    }

    @Test
    fun toStringIncludesStatusAndChallenge() {
        assertEquals(
            "AffirmPrepareAuthResult(status=PENDING, codeChallenge=challenge)",
            AffirmPrepareAuthResult.create(Status.PENDING, "challenge").toString()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun createPendingWithoutChallengeThrows() {
        AffirmPrepareAuthResult.create(Status.PENDING, null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun createUnavailableWithChallengeThrows() {
        AffirmPrepareAuthResult.create(Status.UNAVAILABLE, "challenge")
    }
}
