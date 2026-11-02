
package com.selfhealing.cache.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheTest {

    private LRUCache cache;

    @BeforeEach
    void setUp() {
        // Har test se pehle fresh cache
        cache = new LRUCache(3);
    }

    // Test 1: Insert and retrieve
    @Test
    void testPutAndGet() {

        CacheEntry entry =
                new CacheEntry("A", "Apple", 0);

        cache.put("A", entry);

        CacheEntry result = cache.get("A");

        assertNotNull(result);
        assertEquals("Apple", result.getValue());
    }

    // Test 2: Update existing key
    @Test
    void testUpdateExistingKey() {

        cache.put("A", new CacheEntry("A", "Apple", 0));

        cache.put("A", new CacheEntry("A", "Mango", 0));

        CacheEntry result = cache.get("A");

        assertEquals("Mango", result.getValue());
        assertEquals(1, cache.size());
    }

    // Test 3: Delete an entry
    @Test
    void testDelete() {

        cache.put("A", new CacheEntry("A", "Apple", 0));

        boolean deleted = cache.delete("A");

        assertTrue(deleted);
        assertNull(cache.get("A"));
    }

    // Test 4: Delete non-existing entry
    @Test
    void testDeleteNonExistingKey() {

        boolean deleted = cache.delete("X");

        assertFalse(deleted);
    }

    // Test 5: LRU eviction
    @Test
    void testLRUEviction() {

        cache.put("A", new CacheEntry("A", "Apple", 0));
        cache.put("B", new CacheEntry("B", "Banana", 0));
        cache.put("C", new CacheEntry("C", "Cherry", 0));

        // A becomes recently used
        cache.get("A");

        // Cache full, B should be evicted
        cache.put("D", new CacheEntry("D", "Dates", 0));

        assertNull(cache.get("B"));

        assertNotNull(cache.get("A"));
        assertNotNull(cache.get("C"));
        assertNotNull(cache.get("D"));
    }

    // Test 6: TTL expiration
    @Test
    void testTTLExpiration() throws InterruptedException {

        cache.put("A", new CacheEntry("A", "Apple", 100));

        // Wait for TTL to expire
        Thread.sleep(200);

        CacheEntry result = cache.get("A");

        assertNull(result);
    }

    // Test 7: Clear cache
    @Test
    void testClear() {

        cache.put("A", new CacheEntry("A", "Apple", 0));
        cache.put("B", new CacheEntry("B", "Banana", 0));

        cache.clear();

        assertEquals(0, cache.size());
        assertNull(cache.get("A"));
        assertNull(cache.get("B"));
    }

    // Test 8: Cache size
    @Test
    void testSize() {

        cache.put("A", new CacheEntry("A", "Apple", 0));
        cache.put("B", new CacheEntry("B", "Banana", 0));

        assertEquals(2, cache.size());
    }
}
