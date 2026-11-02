
package com.selfhealing.cache.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CacheEngine {

    private final LRUCache cache;

    // Constructor
    public CacheEngine(
            @Value("${cache.capacity:10000}") int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Cache capacity must be positive"
            );
        }

        this.cache = new LRUCache(capacity);
    }

    // PUT: Store data in cache
    public void put(String key, Object value, long ttlMillis) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Cache key cannot be empty"
            );
        }

        if (value == null) {
            throw new IllegalArgumentException(
                    "Cache value cannot be null"
            );
        }

        if (ttlMillis < 0) {
            throw new IllegalArgumentException(
                    "TTL cannot be negative"
            );
        }

        CacheEntry entry =
                new CacheEntry(key, value, ttlMillis);

        cache.put(key, entry);
    }

    // GET: Retrieve data from cache
    public Object get(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Cache key cannot be empty"
            );
        }

        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return null;
        }

        return entry.getValue();
    }

    // DELETE: Remove a particular entry
    public boolean delete(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Cache key cannot be empty"
            );
        }

        return cache.delete(key);
    }

    // CLEAR: Remove all entries
    public void clear() {

        cache.clear();
    }

    // SIZE: Return active entries count
    public int size() {

        return cache.size();
    }
}
