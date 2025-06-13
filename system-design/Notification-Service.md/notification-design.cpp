#include <iostream>
#include <bits/stdc++.h>

using namespace std;

struct Notification {
    int id;
    string title;
    string message;
    string recipient; // e.g., email or phone number
    
    Notification(int id, const string& title, const string& message, const string& recipient)
        : id(id), title(title), message(message), recipient(recipient) {}
};

class INotificationChannel {
public:
    virtual void send(const Notification& notification) = 0;
    virtual ~INotificationChannel() = default;
};

class EmailChannel : public INotificationChannel {
    public:
    void send(const Notification& notification) override {
        cout << "[EMAIL] To: " << notification.recipient
             << " | Subject: " << notification.title
             << " | Message: " << notification.message << endl;
    }
};

class SMSChannel : public INotificationChannel {
public:
    void send(const Notification& notification) override {
        cout << "[SMS] To: " << notification.recipient
             << " | Message: " << notification.message << endl;
    }
};

class PushChannel : public INotificationChannel {
public:
    void send(const Notification& notification) override {
        cout << "[PUSH] To: " << notification.recipient
             << " | Title: " << notification.title
             << " | Message: " << notification.message << endl;
    }
};

class NotificationService {
    private:
    vector<shared_ptr<INotificationChannel>> channels;
    mutex mtx;
    
    public:
    void addChannel(shared_ptr<INotificationChannel> channel) {
        lock_guard<mutex> lock(mtx);
        channels.push_back(channel);
    }
    
    void sendNotification(const Notification& notification) {
        lock_guard<mutex> lock(mtx);
        for (auto& channel : channels) {
            channel->send(notification);
        }
    }
};

int main()
{
    NotificationService service;
    
    service.addChannel(make_shared<EmailChannel>());
    service.addChannel(make_shared<SMSChannel>());
    service.addChannel(make_shared<PushChannel>());
    
    Notification notification(1, "Login Alert", "New login detected", "user@example.com");
    
    service.sendNotification(notification);
    
    return 0;

}