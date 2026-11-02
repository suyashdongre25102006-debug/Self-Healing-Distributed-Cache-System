
package com.selfhealing.cache.engine;

import java.util.HashMap;
import java.util.Map;

public class LRUCache {

    private static class Node {

        String key;
        CacheEntry entry;

        Node prev;
        Node next;

        Node(String key, CacheEntry entry) {
            this.key = key;
            this.entry = entry;
        }
    }

    private final int capacity;

    private final Map<String, Node> map;

    private final Node head;
    private final Node tail;

    public LRUCache(int capacity) {

        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Capacity must be positive"
            );
        }

        this.capacity = capacity;
        this.map = new HashMap<>();

        head = new Node(null, null);
        tail = new Node(null, null);

        head.next = tail;
        tail.prev = head;
    }

    // Remove node from current position
    private void removeNode(Node node) {

        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    // Insert node after head
    private void addAfterHead(Node node) {

        node.next = head.next;
        node.prev = head;

        head.next.prev = node;
        head.next = node;
    }

    // Move node to most recently used position
    private void moveToHead(Node node) {

        removeNode(node);
        addAfterHead(node);
    }

    // Remove least recently used entry
    private void evictLRU() {

        Node lru = tail.prev;

        if (lru == head) {
            return;
        }

        removeNode(lru);
        map.remove(lru.key);
    }

    // Remove expired entries
    private void cleanExpiredEntriesInternal() {

        Node current = head.next;

        while (current != tail) {

            Node next = current.next;

            if (current.entry.isExpired()) {

                removeNode(current);
                map.remove(current.key);
            }

            current = next;
        }
    }

    // Insert or update cache entry
    public synchronized void put(String key, CacheEntry entry) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Key cannot be empty"
            );
        }

        if (entry == null || !key.equals(entry.getKey())) {
            throw new IllegalArgumentException(
                    "Invalid cache entry"
            );
        }

        // Remove expired entries first
        cleanExpiredEntriesInternal();

        Node node = map.get(key);

        if (node != null) {

            node.entry = entry;
            moveToHead(node);

            return;
        }

        // Evict LRU if capacity is full
        if (map.size() >= capacity) {
            evictLRU();
        }

        Node newNode = new Node(key, entry);

        map.put(key, newNode);
        addAfterHead(newNode);
    }

    // Retrieve entry
    public synchronized CacheEntry get(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Key cannot be empty"
            );
        }

        Node node = map.get(key);

        if (node == null) {
            return null;
        }

        // Check expiration
        if (node.entry.isExpired()) {

            removeNode(node);
            map.remove(key);

            return null;
        }

        // Mark as recently used
        moveToHead(node);

        return node.entry;
    }

    // Delete entry
    public synchronized boolean delete(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Key cannot be empty"
            );
        }

        Node node = map.remove(key);

        if (node == null) {
            return false;
        }

        removeNode(node);

        return true;
    }

    // Return active entry count
    public synchronized int size() {

        cleanExpiredEntriesInternal();

        return map.size();
    }

    // Clear entire cache
    public synchronized void clear() {

        map.clear();

        head.next = tail;
        tail.prev = head;
    }
}
