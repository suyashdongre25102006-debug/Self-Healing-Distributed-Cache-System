
package com.selfhealing.cache.controller;

public class CacheRequest {

    private Object value;
    private long ttlMillis;

    public CacheRequest() {
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public long getTtlMillis() {
        return ttlMillis;
    }

    public void setTtlMillis(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }
}
