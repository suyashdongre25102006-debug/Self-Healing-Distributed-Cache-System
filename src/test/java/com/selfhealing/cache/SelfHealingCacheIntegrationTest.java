
package com.selfhealing.cache;

import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SelfHealingCacheIntegrationTest {

    private static final String NODE_1 =
            "http://localhost:18081";

    private static final String NODE_2 =
            "http://localhost:18082";

    private static final String NODE_3 =
            "http://localhost:18083";

    private static final String COORDINATOR =
            "http://localhost:18080";

    private static final String NODE_URLS =
            NODE_1 + "," + NODE_2 + "," + NODE_3;

    private static final String TEST_KEY =
            "integration-test-key";

    private static final String TEST_VALUE =
            "Self Healing Distributed Cache";

    private static ConfigurableApplicationContext node1;
    private static ConfigurableApplicationContext node2;
    private static ConfigurableApplicationContext node3;
    private static ConfigurableApplicationContext coordinator;

    private static final RestClient client =
            RestClient.create();

    // --------------------------------------------------
    // START FOUR INDEPENDENT SPRING BOOT APPLICATIONS
    // --------------------------------------------------

    @BeforeAll
    static void startCluster() {

        System.out.println("\n================================");
        System.out.println("STARTING DISTRIBUTED CACHE");
        System.out.println("================================\n");

        node1 = startApplication(18081);
        node2 = startApplication(18082);
        node3 = startApplication(18083);

        coordinator = startApplication(18080);

        System.out.println("\nALL FOUR APPLICATIONS STARTED\n");
    }

    private static ConfigurableApplicationContext startApplication(int port) {

        SpringApplication application =
                new SpringApplication(SelfHealingCacheApplication.class);

        application.setDefaultProperties(
                Map.of(
                        "spring.main.banner-mode", "off",
                        "spring.devtools.restart.enabled", "false"
                )
        );

        return application.run(
                "--server.port=" + port,
                "--cache.nodes=" + NODE_URLS
        );
    }

    // --------------------------------------------------
    // TEST 1: NODE HEALTH
    // --------------------------------------------------

    @Test
    @Order(1)
    void testNodeHealth() {

        System.out.println("TEST 1: NODE HEALTH");

        assertNotNull(get(NODE_1 + "/health"));
        assertNotNull(get(NODE_2 + "/health"));
        assertNotNull(get(NODE_3 + "/health"));

        System.out.println("PASS: All three nodes are UP");
    }

    // --------------------------------------------------
    // TEST 2: CLUSTER HEALTH
    // --------------------------------------------------

    @Test
    @Order(2)
    void testClusterHealth() {

        System.out.println("TEST 2: CLUSTER HEALTH");

        Object response =
                get(COORDINATOR + "/cluster/health");

        assertNotNull(response);

        System.out.println("PASS: Cluster health endpoint works");
    }

    // --------------------------------------------------
    // TEST 3: CACHE WRITE
    // --------------------------------------------------

    @Test
    @Order(3)
    void testCacheWrite() {

        System.out.println("TEST 3: CACHE WRITE");

        Map<String, Object> request =
                Map.of(
                        "value", TEST_VALUE,
                        "ttlMillis", 60000
                );

        Object response =
                client.put()
                        .uri(COORDINATOR +
                                "/cluster/cache/" + TEST_KEY)
                        .body(request)
                        .retrieve()
                        .body(Object.class);

        assertNotNull(response);

        System.out.println("PASS: Cache write successful");
    }

    // --------------------------------------------------
    // TEST 4: CACHE READ
    // --------------------------------------------------

    @Test
    @Order(4)
    void testCacheRead() {

        System.out.println("TEST 4: CACHE READ");

        Map<?, ?> response =
                getMap(
                        COORDINATOR +
                                "/cluster/cache/" + TEST_KEY
                );

        assertNotNull(response);
        assertEquals(TEST_VALUE, response.get("value"));

        System.out.println("PASS: Cache read successful");
    }

    // --------------------------------------------------
    // TEST 5: REPLICATION
    // --------------------------------------------------

    @Test
    @Order(5)
    void testReplication() {

        System.out.println("TEST 5: REPLICATION");

        String[] nodes = {
                NODE_1,
                NODE_2,
                NODE_3
        };

        for (String node : nodes) {

            Map<?, ?> response =
                    getMap(
                            node + "/cache/" + TEST_KEY
                    );

            assertNotNull(response);
            assertEquals(
                    TEST_VALUE,
                    response.get("value")
            );

            System.out.println(
                    "PASS: Data found on " + node
            );
        }

        System.out.println(
                "PASS: All three nodes contain the data"
        );
    }

    // --------------------------------------------------
    // TEST 6: CACHE UPDATE
    // --------------------------------------------------

    @Test
    @Order(6)
    void testCacheUpdate() {

        System.out.println("TEST 6: CACHE UPDATE");

        String updatedValue = "Updated Cache Value";

        Map<String, Object> request =
                Map.of(
                        "value", updatedValue,
                        "ttlMillis", 60000
                );

        client.put()
                .uri(COORDINATOR +
                        "/cluster/cache/" + TEST_KEY)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        Map<?, ?> response =
                getMap(
                        COORDINATOR +
                                "/cluster/cache/" + TEST_KEY
                );

        assertEquals(
                updatedValue,
                response.get("value")
        );

        System.out.println("PASS: Cache update successful");
    }

    // --------------------------------------------------
    // TEST 7: TTL EXPIRATION
    // --------------------------------------------------

    @Test
    @Order(7)
    void testTTLExpiration() throws InterruptedException {

        System.out.println("TEST 7: TTL EXPIRATION");

        String key = "ttl-integration-test";

        Map<String, Object> request =
                Map.of(
                        "value", "Temporary Data",
                        "ttlMillis", 2500
                );

        client.put()
                .uri(NODE_1 + "/cache/" + key)
                .body(request)
                .retrieve()
                .toBodilessEntity();

        assertNotNull(
                getMap(NODE_1 + "/cache/" + key)
        );

        Thread.sleep(3500);

        assertNull(
                getOptional(NODE_1 + "/cache/" + key)
        );

        System.out.println("PASS: TTL expiration works");
    }

    // --------------------------------------------------
    // TEST 8: CACHE STATISTICS
    // --------------------------------------------------

    @Test
    @Order(8)
    void testCacheStatistics() {

        System.out.println("TEST 8: CACHE STATISTICS");

        Map<?, ?> response =
                getMap(NODE_1 + "/cache/stats");

        assertNotNull(response);
        assertTrue(response.containsKey("totalRequests"));
        assertTrue(response.containsKey("hits"));
        assertTrue(response.containsKey("misses"));

        System.out.println("PASS: Cache statistics available");
    }

    // --------------------------------------------------
    // TEST 9: CONSISTENT HASHING
    // --------------------------------------------------


    @Test
    @Order(9)
    void testConsistentHashing() {

        System.out.println("TEST 9: CONSISTENT HASHING");

        // Get placement information from coordinator
        Map<?, ?> response = getMap(
                COORDINATOR +
                        "/cluster/management/placement/" +
                        TEST_KEY
        );

        // Verify response
        assertNotNull(response, "Placement response should not be null");

        assertEquals(
                TEST_KEY,
                response.get("key"),
                "Returned key should match requested key"
        );

        // Verify primary node
        String primary = (String) response.get("primary");

        assertNotNull(
                primary,
                "Primary node should not be null"
        );

        assertTrue(
                java.util.List.of(NODE_1, NODE_2, NODE_3)
                        .contains(primary),
                "Primary must be one of the registered nodes"
        );

        // Verify replica nodes
        Object replicasObject = response.get("replicas");

        assertNotNull(
                replicasObject,
                "Replica list should not be null"
        );

        assertInstanceOf(
                java.util.List.class,
                replicasObject
        );

        java.util.List<?> replicas =
                (java.util.List<?>) replicasObject;

        // Replication factor = 3
        // 1 primary + 2 replicas
        assertEquals(
                2,
                replicas.size(),
                "Expected exactly 2 replica nodes"
        );

        // Primary should not be a replica
        assertFalse(
                replicas.contains(primary),
                "Primary node must not appear in replica list"
        );

        // All replicas should be registered nodes
        for (Object replica : replicas) {

            assertTrue(
                    java.util.List.of(NODE_1, NODE_2, NODE_3)
                            .contains(replica),
                    "Replica must belong to registered nodes"
            );
        }

        // No duplicate replicas
        assertEquals(
                replicas.size(),
                new java.util.HashSet<>(replicas).size(),
                "Duplicate replica nodes found"
        );

        System.out.println("Primary Node: " + primary);
        System.out.println("Replica Nodes: " + replicas);

        System.out.println(
                "PASS: Consistent hashing placement verified"
        );
    }


    // --------------------------------------------------
    // TEST 10: NODE FAILURE AND FAILOVER
    // --------------------------------------------------


    @Test
    @Order(10)
    void testNodeFailureAndFailover() throws InterruptedException {

        System.out.println("TEST 10: NODE FAILURE AND FAILOVER");

        // Step 1: Get current placement
        Map<?, ?> placement = getMap(
                COORDINATOR +
                        "/cluster/management/placement/" +
                        TEST_KEY
        );

        assertNotNull(placement);

        String primary = (String) placement.get("primary");

        assertNotNull(
                primary,
                "Primary node cannot be null"
        );

        System.out.println("Selected Primary: " + primary);

        // Step 2: Identify primary application context
        ConfigurableApplicationContext failedNode;

        if (primary.equals(NODE_1)) {

            failedNode = node1;
            node1 = null;

        } else if (primary.equals(NODE_2)) {

            failedNode = node2;
            node2 = null;

        } else if (primary.equals(NODE_3)) {

            failedNode = node3;
            node3 = null;

        } else {

            fail("Unknown primary node: " + primary);
            return;
        }

        assertNotNull(
                failedNode,
                "Primary application context is unavailable"
        );

        // Step 3: Stop primary node
        System.out.println("Stopping primary node: " + primary);

        failedNode.close();

        // Step 4: Wait for heartbeat failure detection
        Thread.sleep(8000);

        System.out.println("Checking failover...");

        // Step 5: Read existing data through coordinator
        Map<?, ?> response = getMap(
                COORDINATOR +
                        "/cluster/cache/" +
                        TEST_KEY
        );

        assertNotNull(response);

        assertEquals(
                "Updated Cache Value",
                response.get("value"),
                "Failover read returned incorrect data"
        );

        System.out.println(
                "PASS: Read failover successful"
        );

        // Step 6: Restart failed node
        int failedPort = Integer.parseInt(
                primary.substring(
                        primary.lastIndexOf(":") + 1
                )
        );

        System.out.println(
                "Restarting node on port: " + failedPort
        );

        ConfigurableApplicationContext restartedNode =
                startApplication(failedPort);

        if (primary.equals(NODE_1)) {

            node1 = restartedNode;

        } else if (primary.equals(NODE_2)) {

            node2 = restartedNode;

        } else {

            node3 = restartedNode;
        }

        // Step 7: Wait for snapshot recovery
        System.out.println(
                "Waiting for node recovery and synchronization..."
        );

        boolean recovered = false;

        for (int i = 0; i < 30; i++) {

            Thread.sleep(1000);

            try {

                Map<?, ?> recoveredData =
                        getMap(
                                primary +
                                        "/cache/" +
                                        TEST_KEY
                        );

                if ("Updated Cache Value".equals(
                        recoveredData.get("value")
                )) {

                    recovered = true;
                    break;
                }

            } catch (Exception ignored) {

                // Node may still be recovering
            }
        }

        // Step 8: Verify recovered data
        assertTrue(
                recovered,
                "Restarted node did not recover the expected cache data"
        );

        System.out.println(
                "PASS: Node recovery and snapshot synchronization verified"
        );
    }


    // --------------------------------------------------
    // HTTP HELPERS
    // --------------------------------------------------

    private static Object get(String url) {

        return client.get()
                .uri(url)
                .retrieve()
                .body(Object.class);
    }

    private static Map<?, ?> getMap(String url) {

        return client.get()
                .uri(url)
                .retrieve()
                .body(Map.class);
    }

    private static Object getOptional(String url) {

        try {

            return get(url);

        } catch (Exception ex) {

            return null;
        }
    }

    // --------------------------------------------------
    // STOP ALL APPLICATIONS
    // --------------------------------------------------

    @AfterAll
    static void stopCluster() {

        System.out.println(
                "\nSTOPPING DISTRIBUTED CACHE CLUSTER"
        );

        closeContext(coordinator);
        closeContext(node1);
        closeContext(node2);
        closeContext(node3);

        System.out.println("ALL APPLICATIONS STOPPED");
    }

    private static void closeContext(
            ConfigurableApplicationContext context) {

        if (context != null && context.isActive()) {
            context.close();
        }
    }
}
