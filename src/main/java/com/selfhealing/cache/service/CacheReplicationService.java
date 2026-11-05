
package com.selfhealing.cache.service;

import com.selfhealing.cache.cluster.NodeRegistry;
import com.selfhealing.cache.controller.CacheRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CacheReplicationService {

    private final NodeRegistry nodeRegistry;
    private final HeartbeatService heartbeatService;
    private final RestClient restClient;

    public CacheReplicationService(
            NodeRegistry nodeRegistry,
            HeartbeatService heartbeatService) {

        this.nodeRegistry = nodeRegistry;
        this.heartbeatService = heartbeatService;
        this.restClient = RestClient.create();
    }

    public List<String> replicate(
            String key,
            CacheRequest request,
            String primaryNode) {

        List<String> successfulNodes = new ArrayList<>();

        // Get replica nodes using consistent hashing
        List<String> replicaNodes =
                nodeRegistry.getReplicaNodes(key, 3);

        for (String node : replicaNodes) {

            // Never replicate to the actual primary
            if (node.equals(primaryNode)) {
                continue;
            }

            // Skip unhealthy or recovering nodes
            if (!heartbeatService.isHealthy(node)) {
                continue;
            }

            try {

                restClient.put()
                        .uri(node + "/cache/" + key)
                        .body(request)
                        .retrieve()
                        .toBodilessEntity();

                successfulNodes.add(node);

                heartbeatService.recordSuccess(node);

            } catch (Exception ex) {

                heartbeatService.recordFailure(node);
            }
        }

        return successfulNodes;
    }

    public Map<String, Object> getReplicationStatus(
            String key,
            String primaryNode,
            List<String> successfulNodes) {

        Map<String, Object> status = new LinkedHashMap<>();

        status.put("key", key);
        status.put("primaryNode", primaryNode);
        status.put("replicaNodes", successfulNodes);
        status.put("replicaCount", successfulNodes.size());
        status.put("totalCopies", successfulNodes.size() + 1);
        status.put("replicationFactor", 3);
        status.put(
                "degraded",
                successfulNodes.size() < 2
        );

        return status;
    }
}
