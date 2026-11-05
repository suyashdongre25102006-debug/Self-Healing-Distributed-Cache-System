package com.selfhealing.cache.engine;

import java.util.concurrent.atomic.AtomicLong;

public class CacheStats {

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);

    public void recordHit() {
        hits.incrementAndGet();
    }

    public void recordMiss() {
        misses.incrementAndGet();
    }

    public void recordEviction() {
        evictions.incrementAndGet();
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public long getEvictions() {
        return evictions.get();
    }

    public long getTotalRequests() {
        return hits.get() + misses.get();
    }

    public double getHitRatio() {
        long total = getTotalRequests();
        return total == 0 ? 0.0 : (double) hits.get() / total;
    }
}