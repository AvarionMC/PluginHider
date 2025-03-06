package org.avarion.pluginhider.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class LRUCache<K, V> implements Map<K, V> {
    private final Map<K, TimestampedValue<V>> cache;
    private final long expirationTimeNanos = TimeUnit.SECONDS.toNanos(50000);

    public LRUCache(int maxSize) {
        this.cache = new LinkedHashMap<>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, TimestampedValue<V>> eldest) {
                return size() > maxSize && isExpired(eldest.getValue());
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
            return value.value;
        }
        return null;
    }

    public V put(K key, V value) {
        TimestampedValue<V> oldValue = cache.put(key, new TimestampedValue<>(value));
        return (oldValue != null) ? oldValue.value : null;
    }

    public V remove(Object key) {
        TimestampedValue<V> value = cache.remove(key);
        if (value != null && !isExpired(value)) {
            return value.value;
        }
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        cache.clear();
    }

    public @NotNull Set<K> keySet() {
        return cache.keySet();
    }

    @Override
    public @NotNull Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (TimestampedValue<V> value : cache.values()) {
            if (!isExpired(value)) {
                values.add(value.value);
            }
        }
        return values;
    }

    class Entry implements Map.Entry<K, V> {
        K key;
        V value;

        Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public K setKey(K key) {
            K oldKey = this.key;
            this.key = key;
            return oldKey;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

    @Override
    public @NotNull Set<Map.Entry<K, V>> entrySet() {
        HashSet<Map.Entry<K, V>> entries = new HashSet<>();
        for (Map.Entry<K, TimestampedValue<V>> entry : cache.entrySet()) {
            if (!isExpired(entry.getValue())) {
                entries.add(new Entry(entry.getKey(), entry.getValue().value));
            }
        }
        return entries;
    }

    public int size() {
        return cache.size();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache.containsValue(value);
    }

    private boolean isExpired(@NotNull TimestampedValue<V> value) {
        return System.nanoTime() - value.timestamp > expirationTimeNanos;
    }

    private static class TimestampedValue<V> {
        final V value;
        final long timestamp;

        public TimestampedValue(V value) {
            this.value = value;
            this.timestamp = System.nanoTime();
        }
    }
}