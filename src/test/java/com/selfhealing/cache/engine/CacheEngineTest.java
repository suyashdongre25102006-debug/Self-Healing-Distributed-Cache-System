
package com.selfhealing.cache.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheEngineTest {

    @Test
    void shouldStoreAndRetrieveData() {

        CacheEngine engine = new CacheEngine(10000);

        engine.put("user1", "Suyash", 0);

        assertEquals("Suyash", engine.get("user1"));
    }

    @Test
    void shouldDeleteData() {

        CacheEngine engine = new CacheEngine(10000);

        engine.put("user1", "Suyash", 0);

        assertTrue(engine.delete("user1"));
        assertNull(engine.get("user1"));
    }

    @Test
    void shouldExpireData() throws InterruptedException {

        CacheEngine engine = new CacheEngine(10000);

        engine.put("user1", "Suyash", 100);

        Thread.sleep(200);

        assertNull(engine.get("user1"));
    }

    @Test
    void shouldClearCache() {

        CacheEngine engine = new CacheEngine(10000);

        engine.put("user1", "Suyash", 0);
        engine.put("user2", "Developer", 0);

        engine.clear();

        assertEquals(0, engine.size());
    }

    @Test
    void shouldReturnNullForMissingKey() {

        CacheEngine engine = new CacheEngine(10000);

        assertNull(engine.get("unknown"));
    }

    @Test
    void shouldReturnCorrectSize() {

        CacheEngine engine = new CacheEngine(10000);

        engine.put("user1", "Suyash", 0);
        engine.put("user2", "Developer", 0);

        assertEquals(2, engine.size());
    }
}
