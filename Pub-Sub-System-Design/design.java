import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


class Message {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    private final String id;
    private final String content;
    private final long timestamp;

    public Message(String content) {
        this.id = String.valueOf(ID_GENERATOR.getAndIncrement());
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getContent() {
        return content;
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

interface Subscriber {

    String getName();

    void onMessage(String topicName, Message message);
}

class SimpleSubscriber implements Subscriber {

    private final String name;

    public SimpleSubscriber(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void onMessage(String topicName, Message message) {
        System.out.println(
                "[" + name + "] received message from topic "
                        + topicName +
                        " => " +
                        message.getContent()
        );
    }
}

class Topic {

    private final String topicName;

    /*

     Publish -> Iterate subscribers
     Subscribe -> Add subscriber

     Optimized for read-heavy workloads.
    */
    
    private final List<Subscriber> subscribers =
            new CopyOnWriteArrayList<>();

    public Topic(String topicName) {
        this.topicName = topicName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void addSubscriber(Subscriber subscriber) {
        subscribers.add(subscriber);
    }

    public List<Subscriber> getSubscribers() {
        return subscribers;
    }
}

class PubSubBroker {

    private final Map<String, Topic> topics =
            new ConcurrentHashMap<>();

    private final ExecutorService executorService =
            Executors.newCachedThreadPool();

    public void createTopic(String topicName) {

        topics.putIfAbsent(
                topicName,
                new Topic(topicName)
        );
    }

    public void subscribe(
            String topicName,
            Subscriber subscriber
    ) {

        Topic topic = topics.get(topicName);

        if (topic == null) {
            throw new IllegalArgumentException(
                    "Topic does not exist"
            );
        }

        topic.addSubscriber(subscriber);
    }

    /*
     Core Logic
     */
    public void publish(
            String topicName,
            Message message
    ) {

        Topic topic = topics.get(topicName);

        if (topic == null) {
            throw new IllegalArgumentException(
                    "Topic does not exist"
            );
        }

        for (Subscriber subscriber :
                topic.getSubscribers()) {

            executorService.submit(() ->
                    subscriber.onMessage(
                            topicName,
                            message
                    )
            );
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }
}

class Publisher {

    private final String name;
    private final PubSubBroker broker;

    public Publisher(
            String name,
            PubSubBroker broker
    ) {
        this.name = name;
        this.broker = broker;
    }

    public void publish(
            String topic,
            String content
    ) {

        System.out.println(
                "Publisher " + name +
                        " published => " +
                        content
        );

        broker.publish(
                topic,
                new Message(content)
        );
    }
}

public class PubSubDemo {

    public static void main(String[] args)
            throws InterruptedException {

        PubSubBroker broker = new PubSubBroker();

        broker.createTopic("sports");
        broker.createTopic("news");

        Subscriber sachin =
                new SimpleSubscriber("Sachin");

        Subscriber rohit =
                new SimpleSubscriber("Rohit");

        Subscriber kiran =
                new SimpleSubscriber("Kiran");

        broker.subscribe("sports", sachin);
        broker.subscribe("sports", rohit);

        broker.subscribe("news", kiran);

        Publisher p1 =
                new Publisher("P1", broker);

        Publisher p2 =
                new Publisher("P2", broker);

        p1.publish(
                "sports",
                "India won the match"
        );

        p2.publish(
                "news",
                "Heavy rain tomorrow"
        );

        Thread.sleep(1000);

        broker.shutdown();
    }
}