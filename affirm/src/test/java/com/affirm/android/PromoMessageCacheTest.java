package com.affirm.android;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PromoMessageCacheTest {

    @Before
    public void setUp() {
        PromoMessageCache.evictAll();
    }

    @After
    public void tearDown() {
        PromoMessageCache.evictAll();
    }

    @Test
    public void remembersBothMessagesForTheSameAmount() {
        PromoMessageCache.put(BigDecimal.TEN, PromoMessageCache.generation(),
                new PromoMessageCache.CachedMessage("ala", "<p>html</p>", true));

        PromoMessageCache.CachedMessage cached = PromoMessageCache.get(BigDecimal.TEN);

        assertNotNull(cached);
        assertEquals("ala", cached.message(false));
        assertEquals("<p>html</p>", cached.message(true));
        assertEquals(true, cached.showPrequal);
    }

    @Test
    public void differentAmountIsAMiss() {
        PromoMessageCache.put(BigDecimal.TEN, PromoMessageCache.generation(),
                new PromoMessageCache.CachedMessage("ala", "<p>html</p>", false));

        assertNull(PromoMessageCache.get(BigDecimal.ONE));
    }

    @Test
    public void amountsWithTheSameCentsShareAnEntry() {
        PromoMessageCache.put(BigDecimal.TEN, PromoMessageCache.generation(),
                new PromoMessageCache.CachedMessage("ala", "<p>html</p>", false));

        assertNotNull(PromoMessageCache.get(new BigDecimal("10.00")));
    }

    @Test
    public void putFromBeforeEvictAllIsDropped() {
        int generation = PromoMessageCache.generation();
        PromoMessageCache.evictAll();

        PromoMessageCache.put(BigDecimal.TEN, generation,
                new PromoMessageCache.CachedMessage("ala", "<p>html</p>", false));

        assertNull(PromoMessageCache.get(BigDecimal.TEN));
    }
}
