
package com.selfhealing.cache.service;

import com.selfhealing.cache.cluster.NodeHealth;
import com.selfhealing.cache.cluster.NodeRegistry;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HeartbeatService {

    private final NodeRegistry nodeRegistry;
    private final RestClient restClient;

    private final Map<String, NodeHealth> nodeHealthMap =
            new ConcurrentHashMap<>();

    public HeartbeatService(NodeRegistry nodeRegistry) {

        this.nodeRegistry = nodeRegistry;
        this.restClient = RestClient.create();

        // Initialize health records for configured nodes
        for (String node : nodeRegistry.getAllNodes()) {

            nodeHealthMap.put(
                    node,
                    new NodeHealth(node)
            );
        }
    }

    // Check node health every 2 seconds
    @Scheduled(fixedRate = 2000)
    public void checkNodes() {

        // Register newly added nodes
        for (String node : nodeRegistry.getAllNodes()) {

            nodeHealthMap.putIfAbsent(
                    node,
                    new NodeHealth(node)
            );
        }

        // Check all registered nodes
        for (String node : nodeRegistry.getAllNodes()) {

            NodeHealth health = nodeHealthMap.get(node);

            try {

                restClient.get()
                        .uri(node + "/health")
                        .retrieve()
                        .toBodilessEntity();

                health.markSuccess();

            } catch (Exception ex) {

                health.markFailure();
            }
        }

        // Remove health records of deleted nodes
        nodeHealthMap.keySet().removeIf(
                node -> !nodeRegistry.getAllNodes().contains(node)
        );
    }

    // Check if node is available for routing
    public boolean isHealthy(String node) {

        NodeHealth health = nodeHealthMap.get(node);

        return health != null
                && health.isHealthy()
                && !health.isRecovering();
    }

    // Check if node is alive, even if recovering
    public boolean isAlive(String node) {

        NodeHealth health = nodeHealthMap.get(node);

        return health != null && health.isHealthy();
    }

    // Record failure during cache operation
    public void recordFailure(String node) {

        NodeHealth health = nodeHealthMap.get(node);

        if (health != null) {
            health.markFailure();
        }
    }

    // Record successful cache operation
    public void recordSuccess(String node) {

        NodeHealth health = nodeHealthMap.get(node);

        if (health != null) {
            health.markSuccess();
        }
    }

    // Get all healthy nodes
    public List<String> getHealthyNodes() {

        return nodeRegistry.getAllNodes()
                .stream()
                .filter(this::isHealthy)
                .toList();
    }

    // Get health record of a particular node
    public NodeHealth getHealth(String node) {

        return nodeHealthMap.get(node);
    }

    // Get health status of all nodes
    public Map<String, NodeHealth> getNodeHealth() {

        return Map.copyOf(nodeHealthMap);
    }
}
