#include <bits/stdc++.h>
#include <iostream>

using namespace std;

class Subscriber {
  public:
  
  string name;
  
  Subscriber(string n) : name(n) {}
  
  virtual void receive(const string& topic, const string& message) {
        cout << "[" << name << "] received on topic [" << topic << "]: " << message << endl;
    }
};

class MessageBroker {
  private:
    unordered_map<string, vector<shared_ptr<Subscriber>>> topicSubscribers;
    mutex mtx;
    
    public:
    void subscribe(const string& topic, shared_ptr<Subscriber> sub) {
        lock_guard<mutex> lock(mtx);
        topicSubscribers[topic].push_back(sub);
        cout << sub->name << " subscribed to " << topic << "\n";
    }
    
    void publish(const string& topic, const string& message) {
        lock_guard<mutex> lock(mtx);
        cout << "\nPublishing on topic [" << topic << "]: " << message << endl;

        if (topicSubscribers.find(topic) == topicSubscribers.end()) return;

        // Deliver messages in separate threads (simulates real-time)
        for (auto& sub : topicSubscribers[topic]) {
            thread([=]() {
                sub->receive(topic, message);
            }).detach();
        }
    }
};

class Publisher {
  MessageBroker &broker;
  string name;
  
  public:
    Publisher(MessageBroker& b, string n) : broker(b), name(n) {}

    void publish(const string& topic, const string& message) {
        cout << name << " is publishing...\n";
        broker.publish(topic, message);
    }
};

int main() {
    MessageBroker broker;

    auto s1 = make_shared<Subscriber>("Alice");
    auto s2 = make_shared<Subscriber>("Bob");
    auto s3 = make_shared<Subscriber>("Charlie");

    broker.subscribe("sports", s1);
    broker.subscribe("sports", s2);
    broker.subscribe("news", s3);
    broker.subscribe("sports", s3);

    Publisher p1(broker, "ESPN");
    Publisher p2(broker, "CNN");

    p1.publish("sports", "India won the match!");
    p2.publish("news", "Election results are out.");
    p1.publish("sports", "Messi scored a hattrick.");

    this_thread::sleep_for(chrono::seconds(1)); // Let all threads finish
    return 0;
}
