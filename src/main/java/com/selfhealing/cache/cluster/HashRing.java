
package com.selfhealing.cache.cluster;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HashRing {

    private static final int VIRTUAL_NODES = 100;

    private final TreeMap<Long, String> ring =
            new TreeMap<>();

    private long hash(String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            return ByteBuffer.wrap(bytes, 0, 8).getLong()
                    & Long.MAX_VALUE;

        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public synchronized void addNode(String node) {

        removeNode(node);

        for (int i = 0; i < VIRTUAL_NODES; i++) {

            String virtualNode = node + "#" + i;

            ring.put(hash(virtualNode), node);
        }
    }

    public synchronized void removeNode(String node) {

        ring.entrySet().removeIf(
                entry -> entry.getValue().equals(node)
        );
    }

    public synchronized List<String> getNodes(
            String key,
            int count) {

        if (ring.isEmpty() || count <= 0) {
            return List.of();
        }

        long keyHash = hash(key);

        Map.Entry<Long, String> start =
                ring.ceilingEntry(keyHash);

        if (start == null) {
            start = ring.firstEntry();
        }

        List<String> result = new ArrayList<>();

        Long position = start.getKey();

        // Traverse ring and collect distinct physical nodes
        for (int i = 0; i < ring.size(); i++) {

            Map.Entry<Long, String> entry =
                    ring.ceilingEntry(position);

            if (entry == null) {
                entry = ring.firstEntry();
            }

            String node = entry.getValue();

            if (!result.contains(node)) {
                result.add(node);
            }

            position = entry.getKey() + 1;

            if (result.size() >= count) {
                break;
            }
        }

        return result;
    }

    public synchronized int getVirtualNodeCount() {
        return ring.size();
    }
}
