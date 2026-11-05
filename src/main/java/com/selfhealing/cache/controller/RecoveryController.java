
package com.selfhealing.cache.controller;

import com.selfhealing.cache.engine.CacheEngine;
import com.selfhealing.cache.engine.CacheSnapshot;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/recovery")
public class RecoveryController {

    private final CacheEngine cacheEngine;

    public RecoveryController(CacheEngine cacheEngine) {
        this.cacheEngine = cacheEngine;
    }

    @GetMapping("/snapshot")
    public CacheSnapshot getSnapshot() {
        return cacheEngine.exportSnapshot();
    }

    @PostMapping("/snapshot")
    public String importSnapshot(
            @RequestBody CacheSnapshot snapshot) {

        cacheEngine.importSnapshot(snapshot);

        return "Snapshot imported successfully";
    }
}
