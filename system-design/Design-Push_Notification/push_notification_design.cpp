#include<bits/stdc++.h>

using namespace std;

enum class ChannelType {
    IOS, ANDROID, EMAIL
};

class Notification {
  public:
  string title;
  string message;
  
  Notification(const string& title, const string& message)
        : title(title), message(message) {}
};

class NotificationChannel {
    public:
    virtual void send(const Notification& notification, const string& userId) = 0;
    virtual ~NotificationChannel() = default;
};

class IOSChannel : public NotificationChannel {
public:
    void send(const Notification& notification, const string& userId) override {
        cout << "[iOS] Sending notification to " << userId << ": " 
             << notification.title << " - " << notification.message << endl;
    }
};


class ANDROIDChannel : public NotificationChannel {
    public:
    void send(const Notification& notification, const string& userId) override {
         cout << "[ANDROID] Sending notification to " << userId << ": " 
             << notification.title << " - " << notification.message << endl;
    }
};

class EmailChannel : public NotificationChannel {
public:
    void send(const Notification& notification, const string& userId) override {
        cout << "[Email] Sending notification to " << userId << ": " 
             << notification.title << " - " << notification.message << endl;
    }
};

class User {
    string id;
    vector<ChannelType> preferredChannels;
    
    public:
    User(string id, vector<ChannelType> channels) :
    id(id), preferredChannels(move(channels)) {}
    
    string getId() const {
        return id;
    }
    
    const vector<ChannelType>& getChannels() const {
        return preferredChannels;
    }
};

class Dispatcher {
    unordered_map<ChannelType, shared_ptr<NotificationChannel>> channelMap;
public:
    Dispatcher() {
        channelMap[ChannelType::IOS] = make_shared<IOSChannel>();
        channelMap[ChannelType::ANDROID] = make_shared<ANDROIDChannel>();
        channelMap[ChannelType::EMAIL] = make_shared<EmailChannel>();
    }

    void dispatch(const Notification& notification, const User& user) {
        for (auto& channel : user.getChannels()) {
            if (channelMap.find(channel) != channelMap.end()) {
                channelMap[channel]->send(notification, user.getId());
            }
        }
    }
};

class EventReceiver {
public:
    Notification receiveEvent() {
        // Simulate receiving a message from the promotions team
        return Notification("Flash Sale!", "Get 50% off on all items today!");
    }
};

class NotificationService {
    Dispatcher dispatcher;
    vector<User> users;
public:
    void registerUser(const User& user) {
        users.push_back(user);
    }

    void processEvent(const Notification& notification) {
        for (const auto& user : users) {
            dispatcher.dispatch(notification, user);
        }
    }
};

int main() {
    NotificationService service;

    // Register users
    service.registerUser(User("user1", {ChannelType::IOS, ChannelType::EMAIL}));
    service.registerUser(User("user2", {ChannelType::ANDROID}));
    service.registerUser(User("user3", {ChannelType::EMAIL, ChannelType::ANDROID, ChannelType::IOS}));

    // Receive event and send notification
    EventReceiver eventReceiver;
    Notification event = eventReceiver.receiveEvent();
    service.processEvent(event);

    return 0;
}