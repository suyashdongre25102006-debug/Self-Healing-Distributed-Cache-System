
package com.selfhealing.cache.cluster;

public class NodeHealth {

    private final String nodeUrl;

    private volatile boolean healthy = true;
    private volatile boolean recovering = false;
    private volatile int failureCount = 0;

    public NodeHealth(String nodeUrl) {
        this.nodeUrl = nodeUrl;
    }

    public String getNodeUrl() {
        return nodeUrl;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public boolean isRecovering() {
        return recovering;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public synchronized void markSuccess() {

        // A returning node needs synchronization
        if (!healthy) {
            recovering = true;
        }

        healthy = true;
        failureCount = 0;
    }

    public synchronized void markFailure() {

        failureCount++;

        if (failureCount >= 3) {
            healthy = false;
        }
    }

    public synchronized void markRecovered() {
        healthy = true;
        recovering = false;
        failureCount = 0;
    }
}
