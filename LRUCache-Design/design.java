import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

class LRUCache {
    private final int capacity;
    private final Map<Integer, Node> map;
    private final DoublyLinkedList dll;
    private final ReentrantLock lock = new ReentrantLock();

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.dll = new DoublyLinkedList();
    }
    
    public int get(int key) {
        lock.lock();
        try {
            if(!map.containsKey(key)) return -1;
            
            Node node = map.get(key);
            dll.moveToFront(node);
            return node.value;
        } finally {
            lock.unlock();
        }
    }
    
    public void put(int key, int value) {
        lock.lock();
        try {
            if (map.containsKey(key)) {
                Node existing = map.get(key);
                existing.value = value;
                dll.moveToFront(existing);
                return;
            }

            if (map.size() == capacity) {
                Node lru = dll.removeLast();
                map.remove(lru.key);
            }

            Node newNode = new Node(key, value);
            dll.addFirst(newNode);
            map.put(key, newNode);
        } finally {
            lock.unlock();
        }
    }
    
    // Node class
    private static class Node {
        int key, value;
        Node prev, next;
        Node(int k, int v) { key = k; value = v; }
    }
    
    // Doubly linked list for LRU order
    private static class DoublyLinkedList {
        private final Node head = new Node(-1, -1);
        private final Node tail = new Node(-1, -1);

        DoublyLinkedList() {
            head.next = tail;
            tail.prev = head;
        }

        void addFirst(Node node) {
            node.next = head.next;
            head.next.prev = node;
            head.next = node;
            node.prev = head;
        }

        void moveToFront(Node node) {
            remove(node);
            addFirst(node);
        }

        Node removeLast() {
            if (tail.prev == head) return null;
            Node node = tail.prev;
            remove(node);
            return node;
        }

        void remove(Node node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
    }
}

public class LRUCacheDemo {
    public static void main(String[] args) {
        LRUCache cache = new LRUCache(3);

        cache.put(1, 10);
        cache.put(2, 20);
        cache.put(3, 30);

        System.out.println(cache.get(1)); // 10 (moves key 1 to MRU)
        
        cache.put(4, 40); // Evicts key 2 (LRU)

        System.out.println(cache.get(2)); // -1 (evicted)
        System.out.println(cache.get(3)); // 30
        System.out.println(cache.get(4)); // 40

        cache.put(5, 50); // Evicts key 1

        System.out.println(cache.get(1)); // -1 (evicted)
        System.out.println(cache.get(5)); // 50
    }
}


