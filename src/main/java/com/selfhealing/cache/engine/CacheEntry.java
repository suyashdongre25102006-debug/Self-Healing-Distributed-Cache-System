
package com.selfhealing.cache.engine;

import lombok.Getter;

@Getter
public class CacheEntry {
    private volatile Object value;
    private volatile long expiryTime;
    private final String key;

    public CacheEntry(String key, Object value, long ttlMillis) {
        this.key = key;
        this.value = value;
        this.expiryTime = calculateExpiry(ttlMillis);
    }

    private long calculateExpiry(long ttlMillis) {
        if (ttlMillis <= 0) {
            return 0; // No expiration
        }

        return System.currentTimeMillis() + ttlMillis;
    }

    public boolean isExpired() {
        return expiryTime != 0 &&
                System.currentTimeMillis() >= expiryTime;
    }

    public void updateValue(Object value, long ttlMillis) {
        this.value = value;
        this.expiryTime = calculateExpiry(ttlMillis);
    }
}
