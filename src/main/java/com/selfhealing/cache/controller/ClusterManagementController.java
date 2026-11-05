
package com.selfhealing.cache.controller;

import com.selfhealing.cache.cluster.NodeRegistry;
import com.selfhealing.cache.service.ConsistentHashingService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cluster/management")
public class ClusterManagementController {

    private final ConsistentHashingService hashingService;
    private final NodeRegistry nodeRegistry;

    public ClusterManagementController(
            ConsistentHashingService hashingService,
            NodeRegistry nodeRegistry) {

        this.hashingService = hashingService;
        this.nodeRegistry = nodeRegistry;
    }

    @PostMapping("/nodes")
    public ResponseEntity<Map<String, Object>> addNode(
            @RequestParam String url) {

        hashingService.addNode(url);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Node registered",
                        "nodes", nodeRegistry.getAllNodes(),
                        "virtualNodes",
                        hashingService.getVirtualNodeCount()
                )
        );
    }

    @DeleteMapping("/nodes")
    public ResponseEntity<Map<String, Object>> removeNode(
            @RequestParam String url) {

        hashingService.removeNode(url);

        return ResponseEntity.ok(
                Map.of(
                        "message", "Node removed",
                        "nodes", nodeRegistry.getAllNodes(),
                        "virtualNodes",
                        hashingService.getVirtualNodeCount()
                )
        );
    }

    @GetMapping("/placement/{key}")
    public ResponseEntity<Map<String, Object>> placement(
            @PathVariable String key) {

        return ResponseEntity.ok(
                Map.of(
                        "key", key,
                        "primary",
                        hashingService.getPrimaryNode(key),
                        "replicas",
                        nodeRegistry.getReplicaNodes(key, 3)
                )
        );
    }
}
