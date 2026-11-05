
package com.selfhealing.cache.engine;

import java.util.List;

public record CacheSnapshot(
        List<SnapshotEntry> entries
) {

    public record SnapshotEntry(
            String key,
            Object value,
            long ttlMillis
    ) {}
}
