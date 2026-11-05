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
    private final CacheStats stats;

    public LRUCache(int capacity, CacheStats stats) {

        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }

        this.capacity = capacity;
        this.stats = stats;
        this.map = new HashMap<>();

        head = new Node(null, null);
        tail = new Node(null, null);

        head.next = tail;
        tail.prev = head;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void addAfterHead(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addAfterHead(node);
    }

    private void evictLRU() {
        Node lru = tail.prev;

        if (lru == head) return;

        removeNode(lru);
        map.remove(lru.key);

        stats.recordEviction();
    }

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

    public synchronized void put(String key, CacheEntry entry) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }

        if (entry == null || !key.equals(entry.getKey())) {
            throw new IllegalArgumentException("Invalid cache entry");
        }

        cleanExpiredEntriesInternal();

        Node node = map.get(key);

        if (node != null) {
            node.entry = entry;
            moveToHead(node);
            return;
        }

        if (map.size() >= capacity) {
            evictLRU();
        }

        Node newNode = new Node(key, entry);

        map.put(key, newNode);
        addAfterHead(newNode);
    }

    public synchronized CacheEntry get(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key cannot be empty");
        }

        Node node = map.get(key);

        if (node == null) {
            return null;
        }

        if (node.entry.isExpired()) {
            removeNode(node);
            map.remove(key);
            return null;
        }

        moveToHead(node);
        return node.entry;
    }

    public synchronized java.util.List<CacheEntry> getAllEntries() {

        cleanExpiredEntriesInternal();

        java.util.List<CacheEntry> entries =
                new java.util.ArrayList<>();

        Node current = head.next;

        while (current != tail) {
            entries.add(current.entry);
            current = current.next;
        }

        return entries;
    }

    public synchronized void putIfAbsent(
            String key,
            CacheEntry entry) {

        cleanExpiredEntriesInternal();

        if (!map.containsKey(key)) {

            if (map.size() >= capacity) {
                evictLRU();
            }

            Node newNode = new Node(key, entry);

            map.put(key, newNode);
            addAfterHead(newNode);
        }
    }

    public synchronized boolean delete(String key) {

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be empty");
        }

        Node node = map.remove(key);

        if (node == null) return false;

        removeNode(node);
        return true;
    }

    public synchronized int size() {
        cleanExpiredEntriesInternal();
        return map.size();
    }

    public synchronized void clear() {
        map.clear();
        head.next = tail;
        tail.prev = head;
    }
}