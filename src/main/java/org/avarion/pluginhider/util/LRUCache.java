package org.avarion.pluginhider.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LRUCache<K, V> implements Map<K, V> {
    private final Map<K, TimestampedValue<V>> cache;
    private final long expirationTimeNanos = TimeUnit.SECONDS.toNanos(50000);

    public LRUCache(int maxSize) {
        this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, TimestampedValue<V>> eldest) {
                return size() > maxSize || isExpired(eldest.getValue());
            }
        };
    }

    public V get(Object key) {
        TimestampedValue<V> value = cache.get(key);
        if (value != null) {
            if (isExpired(value)) {
                //noinspection SuspiciousMethodCalls
                cache.remove(key);
                return null;
            }
            return value.value();
        }
        return null;
    }

    public V put(K key, V value) {
        TimestampedValue<V> oldValue = cache.put(key, new TimestampedValue<>(value));
        return (oldValue != null) ? oldValue.value() : null;
    }

    public V remove(Object key) {
        TimestampedValue<V> value = cache.remove(key);
        if (value != null && !isExpired(value)) {
            return value.value();
        }
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        cache.clear();
    }

    public @NotNull Set<K> keySet() {
        return cache.keySet();
    }

    @Override
    public @NotNull Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    private boolean isExpired(@NotNull TimestampedValue<V> value) {
        return System.nanoTime() - value.timestamp() > expirationTimeNanos;
    }

    private record TimestampedValue<V>(V value, long timestamp) {
        public TimestampedValue(V value) {
            this(value, System.nanoTime());
        }
    }
}