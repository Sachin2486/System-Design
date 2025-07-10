#include <bits/stdc++.h>

using namespace std;

class ISubscriber {
public:

virtual void notify(const string& key, const string& newValue) = 0;
virtual string getId() const = 0;
virtual ~ISubscriber() = default;
};


// User implementation (subscriber)
class User : public ISubscriber {
    string id;
    
    public:
    User(string id) : id(move(id)) {}
    
    void notify(const string& key, const string& newValue) override {
         cout << "User [" << id << "] notified: Configuration [" << key << "] updated to [" << newValue << "]\n";
    }
    
    string getId() const override {
        return id;
    }
};

class ConfigurationManager {
  unordered_map<string, string> configMap;
  unordered_map<string, vector<weak_ptr<ISubscriber>>> subscribersMap;
  
  public:
  void addOrUpdateConfig(const string& key, const string& value) {
      configMap[key] = value;
      notifySubscribers(key);
  }
  
  void deleteConfig(const string& key) {
      if(configMap.erase(key)){
          cout<< "configuration [" << key << "] deleted. \n";
      }else {
          cout << "Configuration [" << key << "] not found.\n";
      }
  }
  
  void searchConfig(const string& key) const {
        auto it = configMap.find(key);
        if (it != configMap.end()) {
            cout << "Found Configuration: [" << key << "] = [" << it->second << "]\n";
        } else {
            cout << "Configuration [" << key << "] not found.\n";
        }
    }

    // Subscribe a user to a config key
    void subscribe(const string& key, shared_ptr<ISubscriber> user) {
        subscribersMap[key].push_back(user);
        cout << "User [" << user->getId() << "] subscribed to configuration [" << key << "]\n";
    }
private:
    void notifySubscribers(const string& key) {
        auto it = subscribersMap.find(key);
        if (it == subscribersMap.end()) return;

        string newValue = configMap[key];

        // Remove expired weak_ptrs while notifying
        vector<weak_ptr<ISubscriber>>& subs = it->second;
        subs.erase(remove_if(subs.begin(), subs.end(),
            [&key, &newValue](weak_ptr<ISubscriber>& wptr) {
                if (auto sptr = wptr.lock()) {
                    sptr->notify(key, newValue);
                    return false;
                }
                return true;
            }), subs.end());
    }
};

int main() {
    ConfigurationManager manager;

    auto user1 = make_shared<User>("Alice");
    auto user2 = make_shared<User>("Bob");

    manager.addOrUpdateConfig("API_TIMEOUT", "30s");

    manager.subscribe("API_TIMEOUT", user1);
    manager.subscribe("API_TIMEOUT", user2);

    manager.addOrUpdateConfig("API_TIMEOUT", "60s"); // both users will be notified

    manager.searchConfig("API_TIMEOUT");

    manager.deleteConfig("API_TIMEOUT");

    return 0;
}
