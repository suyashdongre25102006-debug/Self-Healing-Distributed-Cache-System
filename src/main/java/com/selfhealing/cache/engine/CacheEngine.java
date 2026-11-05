package com.selfhealing.cache.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class CacheEngine {

    private final LRUCache cache;
    private final CacheStats stats;

    public CacheEngine(
            @Value("${cache.capacity:10000}") int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Cache capacity must be positive"
            );
        }

        this.stats = new CacheStats();
        this.cache = new LRUCache(capacity, stats);
    }

    public CacheSnapshot exportSnapshot() {

        List<CacheSnapshot.SnapshotEntry> entries =
                new ArrayList<>();

        for (CacheEntry entry : cache.getAllEntries()) {

            long ttl = entry.getExpiryTime() == 0
                    ? 0
                    : Math.max(
                    1,
                    entry.getExpiryTime() - System.currentTimeMillis()
            );

            entries.add(
                    new CacheSnapshot.SnapshotEntry(
                            entry.getKey(),
                            entry.getValue(),
                            ttl
                    )
            );
        }

        return new CacheSnapshot(entries);
    }

    public void importSnapshot(CacheSnapshot snapshot) {

        if (snapshot == null || snapshot.entries() == null) {
            throw new IllegalArgumentException("Invalid snapshot");
        }

        for (CacheSnapshot.SnapshotEntry entry : snapshot.entries()) {

            if (entry.key() == null || entry.value() == null) {
                continue;
            }

            if (entry.ttlMillis() < 0) {
                continue;
            }

            CacheEntry cacheEntry = new CacheEntry(
                    entry.key(),
                    entry.value(),
                    entry.ttlMillis()
            );

            cache.putIfAbsent(entry.key(), cacheEntry);
        }
    }

    // PUT
    public void put(String key, Object value, long ttlMillis) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key cannot be empty");
        }

        if (value == null) {
            throw new IllegalArgumentException("Cache value cannot be null");
        }

        if (ttlMillis < 0) {
            throw new IllegalArgumentException("TTL cannot be negative");
        }

        CacheEntry entry = new CacheEntry(key, value, ttlMillis);
        cache.put(key, entry);
    }

    // GET
    public Object get(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key cannot be empty");
        }

        CacheEntry entry = cache.get(key);

        if (entry == null) {
            stats.recordMiss();
            return null;
        }

        stats.recordHit();
        return entry.getValue();
    }

    // DELETE
    public boolean delete(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key cannot be empty");
        }

        return cache.delete(key);
    }

    // CLEAR
    public void clear() {
        cache.clear();
    }

    // SIZE
    public int size() {
        return cache.size();
    }

    // STATS
    public CacheStats getStats() {
        return stats;
    }
}