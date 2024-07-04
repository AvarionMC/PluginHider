package org.avarion.plugin_hider.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;

import static org.junit.jupiter.api.Assertions.assertFalse;

class LRUCacheTest {

    private LRUCache<String, String> lruCacheUnderTest;

    @BeforeEach
    void setUp() {
        lruCacheUnderTest = new LRUCache<>(0);
    }

    @Test
    void testRemoveEldestEntry() {
        assertFalse(lruCacheUnderTest.removeEldestEntry(new AbstractMap.SimpleEntry<>("value", "value")));
    }
}
