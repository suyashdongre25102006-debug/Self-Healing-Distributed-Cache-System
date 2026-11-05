
package com.selfhealing.cache.service;

import com.selfhealing.cache.cluster.NodeHealth;
import com.selfhealing.cache.cluster.NodeRegistry;
import com.selfhealing.cache.engine.CacheSnapshot;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class NodeRecoveryService {

    private final NodeRegistry nodeRegistry;
    private final HeartbeatService heartbeatService;
    private final RestClient restClient;

    public NodeRecoveryService(
            NodeRegistry nodeRegistry,
            HeartbeatService heartbeatService) {

        this.nodeRegistry = nodeRegistry;
        this.heartbeatService = heartbeatService;
        this.restClient = RestClient.create();
    }

    @Scheduled(fixedDelay = 5000)
    public void recoverNodes() {

        for (String target : nodeRegistry.getAllNodes()) {

            NodeHealth targetHealth =
                    heartbeatService.getHealth(target);

            if (targetHealth == null
                    || !targetHealth.isHealthy()
                    || !targetHealth.isRecovering()) {
                continue;
            }

            synchronizeNode(target, targetHealth);
        }
    }

    private void synchronizeNode(
            String target,
            NodeHealth targetHealth) {

        List<String> sources =
                heartbeatService.getHealthyNodes();

        for (String source : sources) {

            if (source.equals(target)) {
                continue;
            }

            try {

                // Get snapshot from a healthy source
                CacheSnapshot snapshot = restClient.get()
                        .uri(source + "/internal/recovery/snapshot")
                        .retrieve()
                        .body(CacheSnapshot.class);

                if (snapshot == null) {
                    continue;
                }

                // Import snapshot into recovering node
                restClient.post()
                        .uri(target + "/internal/recovery/snapshot")
                        .body(snapshot)
                        .retrieve()
                        .toBodilessEntity();

                // Rejoin only after successful synchronization
                targetHealth.markRecovered();

                return;

            } catch (Exception ex) {
                // Try another healthy source
            }
        }
    }
}
