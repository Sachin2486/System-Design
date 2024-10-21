#include <bits/stdc++.h>

using namespace std;

template<typename K, typename V>
class Node {
public:
    K key;
    V value;
    Node* prev;
    Node* next;

    Node(K k, V v) : key(k), value(v), prev(nullptr), next(nullptr) {}
};

template<typename K, typename V>
class LruCache {
private:
    int capacity;
    unordered_map<K, Node<K, V>*> cache;
    Node<K, V>* head;
    Node<K, V>* tail;

    void addToHead(Node<K, V>* node) {
        node->prev = head;
        node->next = head->next;
        head->next->prev = node;
        head->next = node;
    }

    void removeNode(Node<K, V>* node) {
        node->prev->next = node->next;
        node->next->prev = node->prev;
    }

    void movetoHead(Node<K, V>* node) {
        removeNode(node);
        addToHead(node);
    }

    Node<K, V>* removeTail() {
        Node<K, V>* node = tail->prev;
        removeNode(node);
        return node;
    }

public:
    LruCache(int capacity) : capacity(capacity) {
        head = new Node<K, V>(K(), V());
        tail = new Node<K, V>(K(), V());
        head->next = tail;
        tail->prev = head;
    }

    ~LruCache() {
        Node<K, V>* current = head;
        while (current != nullptr) {
            Node<K, V>* temp = current;
            current = current->next;
            delete temp;
        }
    }

    V get(K key) {
        if (cache.find(key) == cache.end()) {
            return V();  // Return default value if not found
        }
        Node<K, V>* node = cache[key];
        movetoHead(node);
        return node->value;
    }

    void put(K key, V value) {
        if (cache.find(key) != cache.end()) {
            Node<K, V>* node = cache[key];
            node->value = value;
            movetoHead(node);
        } else {
            Node<K, V>* newNode = new Node<K, V>(key, value);
            cache[key] = newNode;
            addToHead(newNode);

            if (cache.size() > capacity) {
                Node<K, V>* removedNode = removeTail();
                cache.erase(removedNode->key);
                delete removedNode;
            }
        }
    }
};

void LRUCacheDemo() {
    LruCache<int, std::string> cache(3);

    cache.put(1, "Value 1");
    cache.put(2, "Value 2");
    cache.put(3, "Value 3");

    std::cout << cache.get(1) << std::endl;  // Output: Value 1
    std::cout << cache.get(2) << std::endl;  // Output: Value 2

    cache.put(4, "Value 4");

    std::cout << cache.get(3) << std::endl;  // Output: (empty or default)
    std::cout << cache.get(4) << std::endl;  // Output: Value 4

    cache.put(2, "Updated Value 2");

    std::cout << cache.get(1) << std::endl;  // Output: Value 1
    std::cout << cache.get(2) << std::endl;  // Output: Updated Value 2
}

int main() {
    LRUCacheDemo();
    return 0;
}
