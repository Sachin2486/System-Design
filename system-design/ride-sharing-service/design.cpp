#include <bits/stdc++.h>
#include <iostream>

using namespace std;

enum class RideType { 
    REGULAR, 
    PREMIUM
};

enum class RideStatus { 
    REQUESTED, 
    ACCEPTED, 
    IN_PROGRESS, 
    COMPLETED 
};

enum class DriverStatus { 
    AVAILABLE, 
    BUSY
};

struct Location {
    double x, y; // simple coordinate system for demo
    double distanceTo(const Location& other) const {
        return sqrt(pow(x - other.x, 2) + pow(y - other.y, 2));
    }
};

class User {
    protected:
    int id;
    string name;
    
    public:
    User(int id, string name) : id(id), name(move(name)) {}
    virtual ~User() = default;
    
    int getId() const {
        return id;
    }
    
    string getName() const {
        return name;
    }
};

class Passenger : public User {
public:
    Passenger(int id, string name) : User(id, move(name)) {}
};

class Driver : public User {
    Location currentLocation;
    DriverStatus status;
public:
    Driver(int id, string name, Location loc) :
    User(id, move(name)), currentLocation(loc), status(DriverStatus::AVAILABLE) {}
    
    Location getLocation() const {
        return currentLocation;
    }
    
    DriverStatus getStatus() const {
        return status;
    }
    
    void setStatus(DriverStatus s) {
        status = s;
    }
};

class RideRequest {
    shared_ptr<Passenger> passenger;
    Location pickup;
    Location destination;
    RideType type;
    RideStatus status;
    
public:
    RideRequest(shared_ptr<Passenger> p, Location pick, Location dest, RideType t)
        : passenger(move(p)), pickup(pick), destination(dest), type(t), status(RideStatus::REQUESTED) {}

    shared_ptr<Passenger> getPassenger() const { return passenger; }
    Location getPickup() const { return pickup; }
    Location getDestination() const { return destination; }
    RideType getType() const { return type; }
    RideStatus getStatus() const { return status; }
    void setStatus(RideStatus s) { status = s; }
};

class Ride {
    shared_ptr<RideRequest> request;
    shared_ptr<Driver> driver;
    double fare;
public:
    Ride(shared_ptr<RideRequest> req, shared_ptr<Driver> drv)
        : request(move(req)), driver(move(drv)), fare(0) {}

    void calculateFare() {
        double baseFare = (request->getType() == RideType::REGULAR) ? 5.0 : 10.0;
        double distance = request->getPickup().distanceTo(request->getDestination());
        fare = baseFare + (distance * ((request->getType() == RideType::REGULAR) ? 2.0 : 3.5));
    }

    double getFare() const { return fare; }

    void startRide() {
        request->setStatus(RideStatus::IN_PROGRESS);
        driver->setStatus(DriverStatus::BUSY);
        cout << "Ride started for passenger " << request->getPassenger()->getName() << endl;
    }

    void completeRide() {
        request->setStatus(RideStatus::COMPLETED);
        driver->setStatus(DriverStatus::AVAILABLE);
        cout << "Ride completed. Fare: $" << fare << endl;
    }
};

class MatchingEngine {
public:
    static optional<shared_ptr<Driver>> findDriver(const vector<shared_ptr<Driver>>& drivers, const Location& pickup) {
        shared_ptr<Driver> nearestDriver = nullptr;
        double minDistance = numeric_limits<double>::max();
        for (auto& driver : drivers) {
            if (driver->getStatus() == DriverStatus::AVAILABLE) {
                double dist = driver->getLocation().distanceTo(pickup);
                if (dist < minDistance) {
                    minDistance = dist;
                    nearestDriver = driver;
                }
            }
        }
        if (nearestDriver) return nearestDriver;
        return nullopt;
    }
};

class PaymentProcessor {
public:
    void processPayment(const Passenger& passenger, const Driver& driver, double amount) {
        cout << "Processing payment of $" << amount << " from " << passenger.getName()
             << " to " << driver.getName() << endl;
    }
};

class NotificationService {
public:
    void notifyPassenger(const Passenger& p, const string& msg) {
        cout << "[Passenger Notification] " << p.getName() << ": " << msg << endl;
    }
    void notifyDriver(const Driver& d, const string& msg) {
        cout << "[Driver Notification] " << d.getName() << ": " << msg << endl;
    }
};

int main()
{
    auto passenger1 = make_shared<Passenger>(1, "Alice");
    auto driver1 = make_shared<Driver>(101, "Bob", Location{0, 0});
    auto driver2 = make_shared<Driver>(102, "Charlie", Location{5, 5});
    
    vector<shared_ptr<Driver>> drivers = {driver1, driver2};

    // Passenger requests a ride
    auto request = make_shared<RideRequest>(passenger1, Location{1, 1}, Location{10, 10}, RideType::PREMIUM);

    NotificationService notifier;
    auto matchedDriverOpt = MatchingEngine::findDriver(drivers, request->getPickup());
    
    if (matchedDriverOpt) {
        auto matchedDriver = matchedDriverOpt.value();
        request->setStatus(RideStatus::ACCEPTED);
        notifier.notifyPassenger(*passenger1, "Driver " + matchedDriver->getName() + " is on the way.");
        notifier.notifyDriver(*matchedDriver, "Ride request accepted.");

        Ride ride(request, matchedDriver);
        ride.calculateFare();
        ride.startRide();
        ride.completeRide();

        PaymentProcessor payment;
        payment.processPayment(*passenger1, *matchedDriver, ride.getFare());
    } else {
        notifier.notifyPassenger(*passenger1, "No drivers available at the moment.");
    }
    
    return 0;
}