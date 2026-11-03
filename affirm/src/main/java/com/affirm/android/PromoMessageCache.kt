package com.affirm.android

import androidx.collection.LruCache
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger

internal object PromoMessageCache {

    private const val MAX_ENTRIES = 128

    private val cache = LruCache<Int, CachedMessage>(MAX_ENTRIES)

    // Bumped by evictAll so responses to requests sent before a config change are not cached.
    private val generation = AtomicInteger()

    class CachedMessage(
        @JvmField val ala: String,
        @JvmField val htmlAla: String,
        @JvmField val showPrequal: Boolean
    ) {
        fun message(htmlStyle: Boolean): String = if (htmlStyle) htmlAla else ala
    }

    private fun key(amount: BigDecimal): Int {
        return AffirmUtils.decimalDollarsToIntegerCents(amount)
    }

    @JvmStatic
    fun generation(): Int {
        return generation.get()
    }

    @JvmStatic
    @Synchronized
    fun get(amount: BigDecimal): CachedMessage? {
        return cache[key(amount)]
    }

    @JvmStatic
    @Synchronized
    fun put(amount: BigDecimal, generation: Int, message: CachedMessage) {
        if (generation != this.generation.get()) {
            return
        }
        cache.put(key(amount), message)
    }

    @JvmStatic
    @Synchronized
    fun evictAll() {
        cache.evictAll()
        generation.incrementAndGet()
    }
}
