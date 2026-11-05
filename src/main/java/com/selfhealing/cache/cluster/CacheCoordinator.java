
package com.selfhealing.cache.cluster;

import com.selfhealing.cache.controller.CacheRequest;
import com.selfhealing.cache.service.CacheReplicationService;
import com.selfhealing.cache.service.HeartbeatService;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CacheCoordinator {

    private final NodeRegistry nodeRegistry;
    private final CacheReplicationService replicationService;
    private final HeartbeatService heartbeatService;
    private final RestClient restClient;

    public CacheCoordinator(
            NodeRegistry nodeRegistry,
            CacheReplicationService replicationService,
            HeartbeatService heartbeatService) {

        this.nodeRegistry = nodeRegistry;
        this.replicationService = replicationService;
        this.heartbeatService = heartbeatService;

        this.restClient = RestClient.create();
    }

    // WRITE WITH AUTOMATIC FAILOVER
    public Map<String, Object> put(
            String key,
            CacheRequest request) {

        List<String> orderedNodes =
                nodeRegistry.getNodesInOrder(key);

        String actualPrimary = null;

        // Try primary first, then other nodes
        for (String node : orderedNodes) {

            if (!heartbeatService.isHealthy(node)) {
                continue;
            }

            try {

                restClient.put()
                        .uri(node + "/cache/{key}", key)
                        .body(request)
                        .retrieve()
                        .toBodilessEntity();

                actualPrimary = node;

                heartbeatService.recordSuccess(node);

                break;

            } catch (RestClientException ex) {

                // Primary failed. Try another node.
                heartbeatService.recordFailure(node);
            }
        }

        // No node could accept the write
        if (actualPrimary == null) {

            throw new IllegalStateException(
                    "All cache nodes are unavailable"
            );
        }

        // Replicate to remaining healthy nodes
        List<String> successfulReplicas =
                replicationService.replicate(
                        key,
                        request,
                        actualPrimary
                );

        List<String> successfulNodes = new ArrayList<>();

        successfulNodes.add(actualPrimary);
        successfulNodes.addAll(successfulReplicas);

        return Map.of(
                "message", "Data stored successfully",
                "key", key,
                "primaryNode", actualPrimary,
                "successfulNodes", successfulNodes,
                "replicaCount", successfulReplicas.size(),
                "totalCopies", successfulNodes.size(),
                "degraded", successfulNodes.size()
                        < nodeRegistry.getAllNodes().size()
        );
    }

    // READ WITH AUTOMATIC FAILOVER
    public Map<String, Object> get(String key) {

        List<String> nodes =
                nodeRegistry.getNodesInOrder(key);

        for (String node : nodes) {

            if (!heartbeatService.isHealthy(node)) {
                continue;
            }

            try {

                Map<String, Object> result = restClient.get()
                        .uri(node + "/cache/{key}", key)
                        .retrieve()
                        .body(
                                new ParameterizedTypeReference<
                                        Map<String, Object>>() {}
                        );

                if (result != null) {
                    heartbeatService.recordSuccess(node);
                    return result;
                }

            } catch (RestClientException ex) {

                heartbeatService.recordFailure(node);
            }
        }

        return null;
    }

    public String getTargetNode(String key) {

        return nodeRegistry.getNodeForKey(key);
    }
}
