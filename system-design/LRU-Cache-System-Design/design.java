// Requirements Solution:

// put(key, value): Add/update key. If full → evict LRU.

// get(key): Return value if present, else -1. Also mark as most recently used.

// Fixed capacity set during initialization.

// Thread-safe for concurrent access.

// O(1) time for both put and get

import java.util.*;
import java.util.concurrent.locks.*;

// Node for doubly linked list
class Node<K, V> {
    K key;
    V value;
    Node<K, V> prev, next;

    Node(K key, V value) {
        this.key = key;
        this.value = value;
    }
}

class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> cache;
    private final Node<K, V> head, tail; // dummy nodes
    private final ReentrantLock lock;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.lock = new ReentrantLock();

        head = new Node<>(null, null);
        tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    // Get value
    public V get(K key) {
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node == null) return null;

            // Move node to front (MRU)
            remove(node);
            insertToFront(node);
            return node.value;
        } finally {
            lock.unlock();
        }
    }

    // Put value
    public void put(K key, V value) {
        lock.lock();
        try {
            if (cache.containsKey(key)) {
                // Update existing node
                Node<K, V> node = cache.get(key);
                node.value = value;
                remove(node);
                insertToFront(node);
            } else {
                if (cache.size() == capacity) {
                    // Evict LRU from end
                    Node<K, V> lru = tail.prev;
                    remove(lru);
                    cache.remove(lru.key);
                }
                Node<K, V> newNode = new Node<>(key, value);
                cache.put(key, newNode);
                insertToFront(newNode);
            }
        } finally {
            lock.unlock();
        }
    }

    // --- Helper: Remove from linked list ---
    private void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    // --- Helper: Insert node after head (MRU) ---
    private void insertToFront(Node<K, V> node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    // Just for debugging
    public void printCache() {
        Node<K, V> curr = head.next;
        System.out.print("Cache (MRU → LRU): ");
        while (curr != tail) {
            System.out.print(curr.key + "=" + curr.value + " ");
            curr = curr.next;
        }
        System.out.println();
    }
}

    public class LRUCacheDemo {
    public static void main(String[] args) {
        LRUCache<Integer, String> cache = new LRUCache<>(3);

        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        cache.printCache(); // MRU→ 3=C 2=B 1=A

        cache.get(1);       // Access key 1 -> now MRU
        cache.printCache(); // 1=A 3=C 2=B

        cache.put(4, "D");  // Evicts LRU (2)
        cache.printCache(); // 4=D 1=A 3=C

        System.out.println("Get 2: " + cache.get(2)); // null
    }
}



