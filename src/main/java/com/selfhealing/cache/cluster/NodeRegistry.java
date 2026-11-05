
package com.selfhealing.cache.cluster;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class NodeRegistry {

    private final List<String> nodes =
            new CopyOnWriteArrayList<>();

    private final HashRing hashRing = new HashRing();

    public NodeRegistry(
            @Value("${cache.nodes}") List<String> initialNodes) {

        for (String node : initialNodes) {
            registerNode(node);
        }

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one cache node is required"
            );
        }
    }

    public synchronized void registerNode(String node) {

        if (node == null || node.isBlank()) {
            throw new IllegalArgumentException(
                    "Node URL cannot be empty"
            );
        }

        if (nodes.contains(node)) {
            return;
        }

        nodes.add(node);
        hashRing.addNode(node);
    }

    public synchronized void removeNode(String node) {

        if (nodes.remove(node)) {
            hashRing.removeNode(node);
        }
    }

    public String getNodeForKey(String key) {

        return getNodesInOrder(key).stream()
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No cache nodes registered"
                        )
                );
    }

    public List<String> getNodesInOrder(String key) {

        return hashRing.getNodes(key, nodes.size());
    }

    public List<String> getReplicaNodes(
            String key,
            int replicationFactor) {

        List<String> ordered =
                hashRing.getNodes(key, replicationFactor);

        if (ordered.size() <= 1) {
            return List.of();
        }

        return ordered.subList(1, ordered.size());
    }

    public List<String> getAllNodes() {
        return List.copyOf(nodes);
    }

    public int getVirtualNodeCount() {
        return hashRing.getVirtualNodeCount();
    }
}
