#include<bits/stdc++.h>

using namespace std;

enum class RideStatus { REQUESTED, ONGOING, COMPLETED, CANCELLED };
enum class TransportClass { STANDARD, PREMIUM };

class Person {
    public:
    
    string name, phone;
    Person(string n, string p) : name(n), phone(p) {}
    virtual ~Person() {}
};

class User : public Person {
    public:
    int userId;
    vector<int> rideHistory;
    User(int id, string n, string p) : Person(n, p), userId(id) {}
};

class Driver : public Person {
public:
    int driverId;
    string licenseNumber, vehicleInfo;
    bool isAvailable;
    double rating;
    int currentRideId;

    Driver(int id, string n, string p, string lic, string vehicle)
        : Person(n, p), driverId(id), licenseNumber(lic), vehicleInfo(vehicle),
          isAvailable(true), rating(5.0), currentRideId(-1) {}
};

class RideRequest {
public:
    int userId;
    string source, destination;
    vector<string> stops;
    TransportClass tClass;
    RideRequest(int uid, string src, string dst, vector<string> st, TransportClass cls)
        : userId(uid), source(src), destination(dst), stops(st), tClass(cls) {}
};

class Ride {
public:
    int rideId, userId, driverId;
    string source, destination;
    vector<string> stops;
    TransportClass tClass;
    RideStatus status;
    double cost;
    time_t startTime, endTime;

   
    Ride() : rideId(-1), userId(-1), driverId(-1), source(""), destination(""),
          tClass(TransportClass::STANDARD), status(RideStatus::REQUESTED),
          cost(0.0), startTime(0), endTime(0) {}

    Ride(int id, int uId, int dId, string src, string dst, vector<string> st, TransportClass cls)
        : rideId(id), userId(uId), driverId(dId), source(src), destination(dst),
          stops(st), tClass(cls), status(RideStatus::REQUESTED), cost(0.0),
          startTime(0), endTime(0) {}

    void startRide() {
        status = RideStatus::ONGOING;
        startTime = time(nullptr);
    }

    void endRide() {
        status = RideStatus::COMPLETED;
        endTime = time(nullptr);
    }

    void cancelRide() {
        status = RideStatus::CANCELLED;
    }
};


class PricingEngine {
    public:
    static double calculatePrice(Ride& ride, double surgeMultiplier) {
        double baseRate = (ride.tClass == TransportClass::STANDARD) ? 10.0 : 20.0;
        double distance = 5.0 + ride.stops.size() * 2;
        return baseRate * distance * surgeMultiplier;
    }
};

class MatchingEngine {
public:
    static Driver* matchDriver(const vector<Driver*>& drivers) {
        for (auto& d : drivers) {
            if (d->isAvailable) return d;
        }
        return nullptr;
    }
};

class RideManager {
    int rideIdCounter = 1;
    unordered_map<int, Ride> rides;
    vector<Driver*>& drivers;
    unordered_map<int, User*>& users;

public:
    RideManager(vector<Driver*>& d, unordered_map<int, User*>& u)
        : drivers(d), users(u) {}

    int createRide(RideRequest request) {
        Driver* driver = MatchingEngine::matchDriver(drivers);
        if (!driver) {
            cout << "No drivers available.\n";
            return -1;
        }

        int rideId = rideIdCounter++;
        Ride ride(rideId, request.userId, driver->driverId, request.source,
                  request.destination, request.stops, request.tClass);

        double surge = getSurgeMultiplier(); // mock logic
        ride.cost = PricingEngine::calculatePrice(ride, surge);

        rides[rideId] = ride;
        users[request.userId]->rideHistory.push_back(rideId);
        driver->isAvailable = false;
        driver->currentRideId = rideId;

        cout << "Ride created successfully! ID: " << rideId << " | Cost: ₹" << ride.cost << "\n";
        return rideId;
    }

    void startRide(int rideId) {
        if (rides.count(rideId)) rides[rideId].startRide();
    }

    void endRide(int rideId) {
        if (rides.count(rideId)) {
            rides[rideId].endRide();
            getDriverById(rides[rideId].driverId)->isAvailable = true;
            getDriverById(rides[rideId].driverId)->currentRideId = -1;
        }
    }

    void cancelRide(int rideId) {
        if (rides.count(rideId)) {
            rides[rideId].cancelRide();
            getDriverById(rides[rideId].driverId)->isAvailable = true;
        }
    }

    Ride* getRide(int rideId) {
        return (rides.count(rideId)) ? &rides[rideId] : nullptr;
    }

private:
    double getSurgeMultiplier() {
        return 1.0 + (rand() % 2); // Mocked random surge
    }

    Driver* getDriverById(int id) {
        for (auto& d : drivers)
            if (d->driverId == id) return d;
        return nullptr;
    }
};

int main() {
    vector<Driver*> drivers;
    unordered_map<int, User*> users;

    auto user1 = new User(1, "Sachin", "9999");
    auto driver1 = new Driver(101, "Ram", "8888", "DL123", "Swift");
    auto driver2 = new Driver(102, "Shyam", "7777", "DL456", "Innova");

    users[user1->userId] = user1;
    drivers.push_back(driver1);
    drivers.push_back(driver2);

    RideManager rideManager(drivers, users);

    RideRequest req(1, "Koramangala", "Whitefield", {"Marathahalli"}, TransportClass::STANDARD);
    int rideId = rideManager.createRide(req);

    rideManager.startRide(rideId);
    rideManager.endRide(rideId);

    delete user1;
    delete driver1;
    delete driver2;
    return 0;
}
