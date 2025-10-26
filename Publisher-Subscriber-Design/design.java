import java.util.*;
import java.util.concurrent.*;

class Message {
    private final String content;
    
    public Message(String content) {
        this.content = content;
    }
    
    public String getContent() {
        return content;
    }
}

// Subscriber interface
interface Subscriber {
    void notify(Message message);
}

// Concrete Subscriber
class ConcreteSubscriber implements Subscriber {
    private final String name;
    
    public ConcreteSubscriber(String name) {
        this.name = name;
    }
    
    @Override
    public void notify(Message message) {
        System.out.println(name + " received message: " + message.getContent());
    }
}

// Topic class
class Topic {
    private final String name;
    private final List<Subscriber> subscribers;
    
    public Topic(String name) {
        this.name = name;
        this.subscribers = Collections.synchronizedList(new ArrayList<>());
    }
    
    public String getName() {
        return name;
    }
    
    public List<Subscriber> getSubscribers() {
        return subscribers;
    }

    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }
}

//PubSub Service 
class PubSubService {
    private final Map<String, Topic> topics;

    public PubSubService() {
        topics = new ConcurrentHashMap<>();
    }

    public void addTopic(String topicName) {
        topics.putIfAbsent(topicName, new Topic(topicName));
    }

    public void subscribe(String topicName, Subscriber subscriber) {
        topics.putIfAbsent(topicName, new Topic(topicName));
        topics.get(topicName).addSubscriber(subscriber);
    }

    public void publish(String topicName, Message message) {
        Topic topic = topics.get(topicName);
        if (topic != null) {
            synchronized (topic.getSubscribers()) {
                for (Subscriber subscriber : topic.getSubscribers()) {
                    subscriber.notify(message);
                }
            }
        }
    }
}

// Publisher class
class Publisher {
    private final PubSubService service;
    
    public Publisher(PubSubService service) {
        this.service = service;
    } 
    
    public void publish(String topicName, String messageContent) {
        Message message = new Message(messageContent);
        service.publish(topicName, message);
    }
}

// Demo
public class PubSubDemo {
    public static void main(String[] args) {
        PubSubService service = new PubSubService();
        service.addTopic("Sports");
        service.addTopic("Tech");

        Subscriber sub1 = new ConcreteSubscriber("Alice");
        Subscriber sub2 = new ConcreteSubscriber("Bob");

        service.subscribe("Sports", sub1);
        service.subscribe("Sports", sub2);
        service.subscribe("Tech", sub2);

        Publisher publisher = new Publisher(service);
        publisher.publish("Sports", "Football match tonight!");
        publisher.publish("Tech", "New Java version released!");
    }
}