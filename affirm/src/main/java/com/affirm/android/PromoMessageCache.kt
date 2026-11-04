package com.affirm.android

import androidx.collection.LruCache

/**
 * Last promo messages keyed by [PromoRequest.cacheKey], shown while a reconfigured button
 * waits for its response.
 */
internal object PromoMessageCache {

    private const val MAX_ENTRIES = 128

    private val cache = LruCache<String, CachedMessage>(MAX_ENTRIES)

    class CachedMessage(
        @JvmField val ala: String,
        @JvmField val htmlAla: String,
        @JvmField val showPrequal: Boolean
    ) {
        fun message(htmlStyle: Boolean): String = if (htmlStyle) htmlAla else ala
    }

    @JvmStatic
    fun get(key: String): CachedMessage? {
        return cache[key]
    }

    @JvmStatic
    fun put(key: String, message: CachedMessage) {
        cache.put(key, message)
    }

    @JvmStatic
    fun evictAll() {
        cache.evictAll()
    }
}
