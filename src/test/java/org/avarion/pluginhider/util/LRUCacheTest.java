package org.avarion.pluginhider.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheTest {
    @Test
    void putGetContains() {
        LRUCache<String, String> cache = new LRUCache<>(10);
        assertNull(cache.get("missing"));

        cache.put("a", "1");
        assertEquals("1", cache.get("a"));
        assertTrue(cache.containsKey("a"));
        assertEquals(1, cache.size());
    }

    @Test
    void computeIfAbsentCachesResult() {
        LRUCache<String, Integer> cache = new LRUCache<>(10);
        int[] calls = {0};

        int first = cache.computeIfAbsent("k", k -> {
            calls[0]++;
            return 42;
        });
        int second = cache.computeIfAbsent("k", k -> {
            calls[0]++;
            return -1;
        });

        assertEquals(42, first);
        assertEquals(42, second);
        assertEquals(1, calls[0], "mapping function must run only once for the same key");
    }
}
