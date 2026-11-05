
package com.selfhealing.cache.controller;

import com.selfhealing.cache.engine.CacheEngine;
import com.selfhealing.cache.engine.CacheStats;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final CacheEngine cacheEngine;

    public CacheController(CacheEngine cacheEngine) {
        this.cacheEngine = cacheEngine;
    }

    // Store data
    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> put(
            @PathVariable String key,
            @RequestBody CacheRequest request) {

        cacheEngine.put(
                key,
                request.getValue(),
                request.getTtlMillis()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message", "Data stored successfully",
                        "key", key
                )
        );
    }

    // Retrieve data
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> get(
            @PathVariable String key) {

        Object value = cacheEngine.get(key);

        if (value == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                Map.of(
                        "key", key,
                        "value", value
                )
        );
    }

    // Delete particular key
    @DeleteMapping("/{key}")
    public ResponseEntity<Map<String, Object>> delete(
            @PathVariable String key) {

        boolean deleted = cacheEngine.delete(key);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                Map.of(
                        "message", "Data deleted successfully",
                        "key", key
                )
        );
    }

    // Clear entire cache
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clear() {

        cacheEngine.clear();

        return ResponseEntity.ok(
                Map.of(
                        "message", "Cache cleared successfully"
                )
        );
    }

    // Cache statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {

        CacheStats stats = cacheEngine.getStats();

        return ResponseEntity.ok(
                Map.of(
                        "totalEntries", cacheEngine.size(),
                        "totalRequests", stats.getTotalRequests(),
                        "hits", stats.getHits(),
                        "misses", stats.getMisses(),
                        "hitRatio", stats.getHitRatio(),
                        "evictions", stats.getEvictions()
                )
        );
    }
}
