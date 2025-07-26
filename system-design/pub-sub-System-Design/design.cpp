#include <bits/stdc++.h>

using namespace std;

struct Message {
	string Content;
	Message(const std::string& msg) : Content(msg) {}
};

// ---------- Subscriber ----------

class Subscriber {
public:
	Subscriber(const string& name) : name(name) {}

	void receive(const Message& message) {
		lock_guard<mutex> lock(mtx);
        std::cout << "[Subscriber: " << name << "] Received: " << message.Content << "\n";

	}

private:
	string name;
	mutex mtx;
};

//  ---------- Topic ----------

class Topic {
public:
	 void addSubscriber(std::shared_ptr<Subscriber> sub) {
        std::lock_guard<std::mutex> lock(mtx);
        subscribers.insert(sub);
    }

	void removeSubscriber(std::shared_ptr<Subscriber> sub) {
        std::lock_guard<std::mutex> lock(mtx);
        subscribers.erase(sub);
    }

	void publish(const Message& msg) {
		lock_guard<mutex> lock(mtx);
		for (auto& sub : subscribers) {
			std::thread([sub, msg]() {
				sub->receive(msg);
			}).detach(); // Asynchronous, real-time delivery
		}
	}

private:
	mutex mtx;
	unordered_set<shared_ptr<Subscriber>> subscribers;

};

// ---------- Broker ----------
class Broker {
public:
	void registerSubscriber(const std::string& topicName, std::shared_ptr<Subscriber> sub) {
		std::lock_guard<std::mutex> lock(mtx);
		if (topics.find(topicName) == topics.end()) {
			topics[topicName] = std::make_shared<Topic>();
		}
		topics[topicName]->addSubscriber(sub);
	}

	void publish(const std::string& topicName, const std::string& message) {
		std::lock_guard<std::mutex> lock(mtx);
		if (topics.find(topicName) != topics.end()) {
			topics[topicName]->publish(Message(message));
		}
	}

private:
	std::unordered_map<std::string, std::shared_ptr<Topic>> topics;
	std::mutex mtx;
};

// ---------- Publisher ----------
class Publisher {
public:
	Publisher(Broker& broker) : broker(broker) {}

	void publish(const std::string& topic, const std::string& message) {
		broker.publish(topic, message);
	}

private:
	Broker& broker;
};


// ---------- Main ----------
int main() {
	Broker broker;

	// Create subscribers
	auto alice = std::make_shared<Subscriber>("Alice");
	auto bob = std::make_shared<Subscriber>("Bob");
	auto charlie = std::make_shared<Subscriber>("Charlie");

	// Register subscriptions
	broker.registerSubscriber("sports", alice);
	broker.registerSubscriber("sports", bob);
	broker.registerSubscriber("movies", charlie);
	broker.registerSubscriber("sports", charlie);

	// Create publishers
	Publisher sportsPublisher(broker);
	Publisher moviePublisher(broker);

	// Publish messages
	sportsPublisher.publish("sports", "India won the cricket match!");
	sportsPublisher.publish("sports", "Messi scored a last-minute goal!");
	moviePublisher.publish("movies", "Oppenheimer released worldwide!");

	// Let async threads finish
	std::this_thread::sleep_for(std::chrono::seconds(1));

	return 0;
}