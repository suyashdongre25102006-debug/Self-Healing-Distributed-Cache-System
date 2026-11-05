
package com.selfhealing.cache.controller;

import com.selfhealing.cache.cluster.CacheCoordinator;
import com.selfhealing.cache.cluster.NodeRegistry;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cluster/cache")
public class CoordinatorController {

    private final CacheCoordinator coordinator;
    private final NodeRegistry nodeRegistry;

    public CoordinatorController(
            CacheCoordinator coordinator,
            NodeRegistry nodeRegistry) {

        this.coordinator = coordinator;
        this.nodeRegistry = nodeRegistry;
    }

    @PutMapping("/{key}")
    public ResponseEntity<Map<String, Object>> put(
            @PathVariable String key,
            @RequestBody CacheRequest request) {

        Map<String, Object> result =
                coordinator.put(key, request);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> get(
            @PathVariable String key) {

        Map<String, Object> result = coordinator.get(key);

        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/nodes")
    public ResponseEntity<Map<String, Object>> nodes() {

        return ResponseEntity.ok(Map.of(
                "totalNodes", nodeRegistry.getAllNodes().size(),
                "nodes", nodeRegistry.getAllNodes()
        ));
    }
}
