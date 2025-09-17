// Topic — Represents a channel where messages are published and subscribers are attached.

// Subscriber — Represents an entity that listens to a topic and processes incoming messages.

// Publisher — Represents an entity that can publish messages to a topic.

// PubSubService — Central coordinator that manages topics, subscriptions, and message delivery.

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// ------------------- Subscriber -------------------

interface Subscriber {
    void onMessage(String topic, String message);
    String getId();
}

class BasicSubscriber implements Subscriber {
    private static final AtomicInteger idGenerator = new AtomicInteger();
    private final String id;
    
    public BasicSubscriber() {
        this.id = "Sub-" + idGenerator.incrementAndGet();
    }
    
    @Override
    public void onMessage(String topic, String message) {
        System.out.println("[" + id + "] received on topic '" + topic + "': " + message);
    }
    
    @Override
    public String getId() {
        return id;
    }
}

// ------------------- Topic -------------------
class Topic {
    private final String name;
    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();
    
    public Topic(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void addSubscriber(Subscriber sub) {
        subscribers.add(sub);
    }
    
    public void removeSubscriber(Subscriber sub) {
        subscribers.remove(sub);
    }
    
    public List<Subscriber> getSubscribers() {
        return subscribers;
    }
}

// ------------------- PubSubService -------------------
class PubSubService {
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public void createTopic(String name) {
        topics.putIfAbsent(name, new Topic(name));
    }
    
    public void subscribe(String topicName, Subscriber sub) {
        Topic topic = topics.get(topicName);
        if(topic != null) {
            topic.addSubscriber(sub);
            System.out.println(sub.getId() + " subscribed to " + topicName);
        } else {
            System.out.println("Topic not found: " + topicName);
        }
    }
    
    public void unsubscribe(String topicName, Subscriber sub) {
        Topic topic = topics.get(topicName);
        if (topic != null) {
            topic.removeSubscriber(sub);
            System.out.println(sub.getId() + " unsubscribed from " + topicName);
        }
    }
    
    public void publish(String topicName, String message) {
        Topic topic = topics.get(topicName);
        if (topic != null) {
            for (Subscriber sub : topic.getSubscribers()) {
                executor.submit(() -> sub.onMessage(topicName, message));
            }
        } else {
            System.out.println("Topic not found: " + topicName);
        }
    }
}

// ------------------- Publisher -------------------
class Publisher {
    private static final AtomicInteger idGenerator = new AtomicInteger();
    private final String id;
    private final PubSubService service;

    public Publisher(PubSubService service) {
        this.id = "Pub-" + idGenerator.incrementAndGet();
        this.service = service;
    }

    public void publish(String topic, String message) {
        System.out.println("[" + id + "] publishing to " + topic + ": " + message);
        service.publish(topic, message);
    }
}

// ------------------- Demo -------------------
public class PubSubDemo {
    public static void main(String[] args) throws InterruptedException {
        PubSubService service = new PubSubService();

        // Create topics
        service.createTopic("Sports");
        service.createTopic("Tech");

        // Subscribers
        Subscriber s1 = new BasicSubscriber();
        Subscriber s2 = new BasicSubscriber();
        Subscriber s3 = new BasicSubscriber();

        service.subscribe("Sports", s1);
        service.subscribe("Sports", s2);
        service.subscribe("Tech", s2);
        service.subscribe("Tech", s3);

        // Publishers
        Publisher p1 = new Publisher(service);
        Publisher p2 = new Publisher(service);

        p1.publish("Sports", "India won the cricket match!");
        p2.publish("Tech", "New Java version released!");

        Thread.sleep(1000); // give time for async messages to deliver
    }
}