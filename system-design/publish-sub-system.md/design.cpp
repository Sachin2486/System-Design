#include <iostream>
#include <unordered_map>
#include <vector>
#include <string>
#include <mutex>
#include <thread>
#include <queue>
#include <condition_variable>
#include <functional>
#include <memory>
#include <atomic>

using namespace std;

// Forward declaration
class Topic;

// Subscriber class
class Subscriber {
public:
    using Callback = function<void(const string&)>;

    Subscriber(string name, Callback cb) 
        : name_(move(name)), callback_(move(cb)), active_(true) {}

    void receive(const string& message) {
        if (active_) callback_(message);
    }

    void deactivate() { active_ = false; }

    string getName() const { return name_; }

private:
    string name_;
    Callback callback_;
    atomic<bool> active_;
};

// Topic class (thread-safe message queue)
class Topic {
public:
    Topic(string name) : name_(move(name)), stop_(false) {
        deliveryThread_ = thread(&Topic::deliverMessages, this);
    }

    ~Topic() {
        {
            unique_lock<mutex> lock(mtx_);
            stop_ = true;
        }
        cv_.notify_all();
        if (deliveryThread_.joinable())
            deliveryThread_.join();
    }

    void subscribe(shared_ptr<Subscriber> sub) {
        lock_guard<mutex> lock(mtx_);
        subscribers_.push_back(sub);
    }

    void publish(const string& message) {
        {
            lock_guard<mutex> lock(mtx_);
            messageQueue_.push(message);
        }
        cv_.notify_one();
    }

private:
    void deliverMessages() {
        while (true) {
            unique_lock<mutex> lock(mtx_);
            cv_.wait(lock, [this] { return !messageQueue_.empty() || stop_; });

            if (stop_ && messageQueue_.empty())
                break;

            string msg = move(messageQueue_.front());
            messageQueue_.pop();

            // Copy subscribers to avoid holding lock during callbacks
            auto subscribersCopy = subscribers_;
            lock.unlock();

            for (auto& sub : subscribersCopy) {
                sub->receive(msg);
            }
        }
    }

    string name_;
    vector<shared_ptr<Subscriber>> subscribers_;
    queue<string> messageQueue_;
    mutex mtx_;
    condition_variable cv_;
    thread deliveryThread_;
    bool stop_;
};

// Broker class (manages topics)
class Broker {
public:
    void createTopic(const string& topicName) {
        lock_guard<mutex> lock(mtx_);
        if (topics_.find(topicName) == topics_.end()) {
            topics_[topicName] = make_shared<Topic>(topicName);
        }
    }

    void subscribe(const string& topicName, shared_ptr<Subscriber> sub) {
        lock_guard<mutex> lock(mtx_);
        if (topics_.find(topicName) != topics_.end()) {
            topics_[topicName]->subscribe(sub);
        }
    }

    void publish(const string& topicName, const string& message) {
        lock_guard<mutex> lock(mtx_);
        if (topics_.find(topicName) != topics_.end()) {
            topics_[topicName]->publish(message);
        }
    }

private:
    unordered_map<string, shared_ptr<Topic>> topics_;
    mutex mtx_;
};

// Example usage
int main() {
    Broker broker;

    // Create topics
    broker.createTopic("sports");
    broker.createTopic("news");

    // Create subscribers
    auto sub1 = make_shared<Subscriber>("Alice", [](const string& msg) {
        cout << "[Alice] Received: " << msg << endl;
    });

    auto sub2 = make_shared<Subscriber>("Bob", [](const string& msg) {
        cout << "[Bob] Received: " << msg << endl;
    });

    // Subscribe to topics
    broker.subscribe("sports", sub1);
    broker.subscribe("sports", sub2);
    broker.subscribe("news", sub1);

    // Multiple publishers (threads)
    thread pub1([&] {
        broker.publish("sports", "Sports Update 1");
        broker.publish("sports", "Sports Update 2");
    });

    thread pub2([&] {
        broker.publish("news", "Breaking News 1");
        broker.publish("news", "Breaking News 2");
    });

    pub1.join();
    pub2.join();

    // Allow delivery before program exit
    this_thread::sleep_for(chrono::seconds(1));

    return 0;
}
