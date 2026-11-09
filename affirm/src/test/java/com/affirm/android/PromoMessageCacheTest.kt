package com.affirm.android

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class PromoMessageCacheTest {

    @Before
    fun setUp() {
        PromoMessageCache.evictAll()
    }

    @After
    fun tearDown() {
        PromoMessageCache.evictAll()
    }

    @Test
    fun remembersBothMessagesForAKey() {
        PromoMessageCache.put("a", message(showPrequal = true))

        val cached = PromoMessageCache.get("a")

        assertThat(cached).isNotNull()
        assertThat(cached!!.message(false)).isEqualTo("ala")
        assertThat(cached.message(true)).isEqualTo("<p>html</p>")
        assertThat(cached.showPrequal).isTrue()
    }

    @Test
    fun differentKeyIsAMiss() {
        PromoMessageCache.put("a", message())

        assertThat(PromoMessageCache.get("b")).isNull()
    }

    @Test
    fun evictAllClearsEntries() {
        PromoMessageCache.put("a", message())

        PromoMessageCache.evictAll()

        assertThat(PromoMessageCache.get("a")).isNull()
    }

    @Test
    fun evictsLeastRecentlyUsedBeyond256Entries() {
        (0 until 256).forEach { PromoMessageCache.put("key$it", message()) }
        PromoMessageCache.get("key0")

        PromoMessageCache.put("key256", message())

        assertThat(PromoMessageCache.get("key0")).isNotNull()
        assertThat(PromoMessageCache.get("key1")).isNull()
        assertThat(PromoMessageCache.get("key256")).isNotNull()
    }

    private fun message(showPrequal: Boolean = false) =
        PromoMessageCache.CachedMessage("ala", "<p>html</p>", showPrequal)
}
