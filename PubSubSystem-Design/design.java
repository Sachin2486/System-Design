import java.util.*;
import java.util.concurrent.*;

// Topic class -> Represents a communication channel where messages are published
class Topic {
	private final String name;
	private final List<Subscriber> subscribers;

	public Topic(String name) {
		this.name = name;
		this.subscribers = new CopyOnWriteArrayList<>(); // Thread-safe list
	}

	public String getName() {
		return name;
	}

	public void addSubscriber(Subscriber subscriber) {
		subscribers.add(subscriber);
	}

	public List<Subscriber> getSubscribers() {
		return subscribers;
	}
}

// Subscriber interface -> Defines how subscribers receive messages
interface Subscriber {
    void onMessage(String topic, String message);
}

// Publisher class -> Publishes messages to a topic
class Publisher {
    private final Broker broker;
    
    public Publisher(Broker broker) {
        this.broker = broker;
    }
    
    public void publish(String topicName, String message) {
        broker.publish(topicName, message);
    }
}

// Broker class -> Central system managing topics, publishers, and subscribers
class Broker {
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // Create or get existing topic
    public Topic createTopic(String name) {
        return topics.computeIfAbsent(name, Topic::new);
    }
    
    // Subscribe a subscriber to a topic
    public void subscribe(String topicName, Subscriber subscriber) {
        Topic topic = createTopic(topicName);
        topic.addSubscriber(subscriber);
    }
    
    public void publish(String topicName, String message) {
        Topic topic = topics.get(topicName);
        if(topic == null) {
           System.out.println("Topic " + topicName + " does not exist.");
            return; 
        }
        
        for(Subscriber subscriber : topic.getSubscribers()) {
            executorService.submit(() -> subscriber.onMessage(topicName, message));
        }
    }
}

// Concrete subscriber -> Implements how subscribers handle messages
class ConcreteSubscriber implements Subscriber {
    private final String name;
    
    public ConcreteSubscriber(String name) {
        this.name = name;
    }
    
    @Override
    public void onMessage(String topic, String message) {
        System.out.println("Subscriber [" + name + "] received on topic '" + topic + "': " + message);
    }
}

// Demo class -> Runs the Pub-Sub system
public class PubSubSystemDemo {
    public static void main(String[] args) {
        Broker broker = new Broker();

        // Create publishers
        Publisher publisher1 = new Publisher(broker);
        Publisher publisher2 = new Publisher(broker);

        // Create subscribers
        Subscriber sub1 = new ConcreteSubscriber("Alice");
        Subscriber sub2 = new ConcreteSubscriber("Bob");
        Subscriber sub3 = new ConcreteSubscriber("Charlie");

        // Subscriptions
        broker.subscribe("Sports", sub1);
        broker.subscribe("Sports", sub2);
        broker.subscribe("News", sub2);
        broker.subscribe("News", sub3);

        // Publish messages
        publisher1.publish("Sports", "India won the cricket match!");
        publisher2.publish("News", "Stock market hits all-time high.");
        publisher1.publish("Sports", "Football World Cup is coming soon!");
    }
}