import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

class PubSubBroker {

    private final Map<String, List<Subscriber>> topicSubscribers;
    private final ExecutorService executor;
    private final ReentrantLock lock = new ReentrantLock();

    public PubSubBroker() {
        this.topicSubscribers = new HashMap<>();
        this.executor = Executors.newCachedThreadPool();
    }

    // Subscribe
    public void subscribe(String topic, Subscriber subscriber) {
        lock.lock();
        try {
            topicSubscribers.putIfAbsent(topic, new ArrayList<>());
            List<Subscriber> subs = topicSubscribers.get(topic);

            if (!subs.contains(subscriber)) {
                subs.add(subscriber);
            }
        } finally {
            lock.unlock();
        }
    }

    // Publish message to a topic
    public void publish(String topic, String message) {
        lock.lock();
        List<Subscriber> subs = null;
        try {
            subs = topicSubscribers.getOrDefault(topic, Collections.emptyList());
        } finally {
            lock.unlock();
        }

        for (Subscriber s : subs) {
            executor.submit(() -> s.onMessage(topic, message));
        }
    }

    // Shutdown (clean exit)
    public void shutdown() {
        executor.shutdown();
    }

    // Subscriber interface
    interface Subscriber {
        void onMessage(String topic, String message);
        String getName();
    }
}

// Concrete subscriber class
class SimpleSubscriber implements PubSubBroker.Subscriber {

    private final String name;

    public SimpleSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void onMessage(String topic, String message) {
        System.out.println("[" + name + "] received -> Topic: " + topic + ", Message: " + message);
    }

    @Override
    public String getName() {
        return name;
    }
}

// Publisher class
class Publisher {

    private final String name;
    private final PubSubBroker broker;

    public Publisher(String name, PubSubBroker broker) {
        this.name = name;
        this.broker = broker;
    }

    public void publish(String topic, String message) {
        System.out.println("Publisher " + name + " publishing to '" + topic + "'");
        broker.publish(topic, message);
    }
}

public class PubSubDemo {
    
    public static void main(String[] args) throws InterruptedException {

        PubSubBroker broker = new PubSubBroker();

        // Subscribers
        PubSubBroker.Subscriber s1 = new SimpleSubscriber("Sachin");
        PubSubBroker.Subscriber s2 = new SimpleSubscriber("Rohit");
        PubSubBroker.Subscriber s3 = new SimpleSubscriber("Kiran");

        broker.subscribe("sports", s1);
        broker.subscribe("sports", s2);
        broker.subscribe("news", s3);

        // Publishers
        Publisher p1 = new Publisher("P1", broker);
        Publisher p2 = new Publisher("P2", broker);

        // Publishing
        p1.publish("sports", "India won the match!");
        p2.publish("news", "Heavy rain expected tomorrow.");

        Thread.sleep(500); // Wait for async tasks

        broker.shutdown();
    }
}
