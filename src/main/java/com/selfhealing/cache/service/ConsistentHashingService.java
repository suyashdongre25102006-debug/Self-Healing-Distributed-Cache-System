
package com.selfhealing.cache.service;

import com.selfhealing.cache.cluster.NodeRegistry;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsistentHashingService {

    private static final int REPLICATION_FACTOR = 3;

    private final NodeRegistry nodeRegistry;

    public ConsistentHashingService(
            NodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
    }

    public String getPrimaryNode(String key) {
        return nodeRegistry.getNodeForKey(key);
    }

    public List<String> getNodesForKey(String key) {
        return nodeRegistry.getNodesInOrder(key)
                .stream()
                .limit(REPLICATION_FACTOR)
                .toList();
    }

    public void addNode(String node) {
        nodeRegistry.registerNode(node);
    }

    public void removeNode(String node) {
        nodeRegistry.removeNode(node);
    }

    public int getVirtualNodeCount() {
        return nodeRegistry.getVirtualNodeCount();
    }
}
