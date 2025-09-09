// classes Invloved ///
// Message
// Content of the message, timestamp.

// Topic
// Name of the topic, list of subscribers.

// Subscriber (interface)
// Method to receive messages.
// Different subscribers can implement this (e.g., ConsoleSubscriber, FileSubscriber).

// Publisher
// Publishes messages to a topic via PubSubService.

// PubSubService (Facade)
// Manages topics, subscriptions, and message delivery.

import java.util.* ;
import java.util.concurrent.*;

class Message {
    private final String content;
    private final long timestamp;
    
    public Message(String content) {
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getContent() {
        return content;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return "[" + timestamp + "] " + content;
    }
}

// --- Subscriber Interface ---
interface Subscriber {
   void receive(Message message, String topic);
    String getId();
}

class ConsoleSubscriber implements Subscriber {
    private final String id;
    
    public ConsoleSubscriber(String id) {
        this.id = id;
    }
    
    public void receive(Message message, String topic) {
        System.out.println("Subscriber " + id + " received on topic '" + topic + "': " + message);
    }
    
    public String getId() {
        return id;
    }
}

// --- Topic ---
class Topic {
    private final String name;
    private final Set<Subscriber> subscribers;
    
    public Topic(String name) {
        this.name = name;
        this.subscribers = ConcurrentHashMap.newKeySet(); // thread-safe set
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
    
    public Set<Subscriber> getSubscribers() {
        return subscribers;
    }
}

// --- PubSubService (Main Engine) ---
class PubSubService {
    private final Map<String, Topic> topics;
    
    public PubSubService() {
        this.topics = new ConcurrentHashMap<>();
    }
    
    // Create or get topic
    private Topic getOrCreateTopic(String topicName) {
        return topics.computeIfAbsent(topicName, Topic::new);
    }
    
    // Subscribe
    public void subscribe(String topicName, Subscriber subscriber) {
        Topic topic = getOrCreateTopic(topicName);
        topic.addSubscriber(subscriber);
        System.out.println("Subscriber " + subscriber.getId() + " subscribed to topic " + topicName);
    }
    
    // Unsubscribe
    public void unsubscribe(String topicName, Subscriber subscriber) {
        Topic topic = topics.get(topicName);
        if (topic != null) {
            topic.removeSubscriber(subscriber);
            System.out.println("Subscriber " + subscriber.getId() + " unsubscribed from topic " + topicName);
        }
    }
    
    // Publish Message
    public void publish(String topicName, Message message) {
        Topic topic = getOrCreateTopic(topicName); 
        System.out.println("Publishing message to topic " + topicName + ": " + message);
        
        // Deliver asynchronously to avoid blocking publisher
        for (Subscriber sub : topic.getSubscribers()) {
            CompletableFuture.runAsync(() -> sub.receive(message, topicName));
        }
    }

}

class Publisher {
    private final String id;
    private final PubSubService service; 
    
    public Publisher(String id, PubSubService service) {
        this.id = id;
        this.service = service;
    }
    
    public void publish(String topic, String content) {
        Message msg = new Message(content);
        service.publish(topic,msg);
    }
    
}

// --- Demo ---
public class PubSubDemo {
    public static void main(String[] args) throws InterruptedException {
        PubSubService service = new PubSubService();

        // Subscribers
        Subscriber s1 = new ConsoleSubscriber("S1");
        Subscriber s2 = new ConsoleSubscriber("S2");
        Subscriber s3 = new ConsoleSubscriber("S3");

        // Subscribe them
        service.subscribe("sports", s1);
        service.subscribe("sports", s2);
        service.subscribe("news", s2);
        service.subscribe("news", s3);

        // Publishers
        Publisher p1 = new Publisher("P1", service);
        Publisher p2 = new Publisher("P2", service);

        // Publish messages
        p1.publish("sports", "Team A won the match!");
        p2.publish("news", "Breaking news: Market hits record high!");

        // Allow async delivery to finish
        Thread.sleep(1000);
    }
}