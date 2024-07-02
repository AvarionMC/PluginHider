package org.avarion.plugin_hider.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    public LRUCache(int maxSize) {
        super(maxSize, 0.75f, true);
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (size() > maxSize) {
            return true;
        }
        if (eldest.getValue() instanceof ReceivedPackets) {
            return ((ReceivedPackets) eldest.getValue()).isStale();
        }
        return false;
    }
}
