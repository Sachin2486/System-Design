#include <bits/stdc++.h>
using namespace std;

enum class VehicleType { Car, Motorcycle, Truck };
enum class SpotType { Car, Motorcycle, Truck };

// ---------- Vehicle Base and Derived Classes ----------
class Vehicle {
protected:
    string licensePlate;
    VehicleType type;

public:
    Vehicle(string plate, VehicleType t) : licensePlate(plate), type(t) {}
    virtual ~Vehicle() = default;

    VehicleType getType() const { return type; }
    string getPlate() const { return licensePlate; }
};

class Car : public Vehicle {
public:
    Car(string plate) : Vehicle(plate, VehicleType::Car) {}
};

class Motorcycle : public Vehicle {
public:
    Motorcycle(string plate) : Vehicle(plate, VehicleType::Motorcycle) {}
};

class Truck : public Vehicle {
public:
    Truck(string plate) : Vehicle(plate, VehicleType::Truck) {}
};

// ---------- Parking Spot ----------
class ParkingSpot {
    int id;
    SpotType type;
    bool occupied;
    shared_ptr<Vehicle> vehicle;
    mutex mtx;

public:
    ParkingSpot(int id, SpotType type) : id(id), type(type), occupied(false), vehicle(nullptr) {}

    bool assignVehicle(shared_ptr<Vehicle> v) {
        lock_guard<mutex> lock(mtx);
        if (!occupied && isCompatible(v->getType())) {
            vehicle = v;
            occupied = true;
            return true;
        }
        return false;
    }

    void removeVehicle() {
        lock_guard<mutex> lock(mtx);
        vehicle = nullptr;
        occupied = false;
    }

    bool isAvailable() {
        lock_guard<mutex> lock(mtx);
        return !occupied;
    }

    bool isCompatible(VehicleType vt) {
        return static_cast<int>(vt) == static_cast<int>(type);
    }

    int getId() const { return id; }
    SpotType getType() const { return type; }
};

// ---------- Level ----------
class Level {
    int levelNumber;
    vector<shared_ptr<ParkingSpot>> spots;

public:
    Level(int num, int carSpots, int motorcycleSpots, int truckSpots) : levelNumber(num) {
        int id = 0;
        for (int i = 0; i < carSpots; ++i)
            spots.push_back(make_shared<ParkingSpot>(id++, SpotType::Car));
        for (int i = 0; i < motorcycleSpots; ++i)
            spots.push_back(make_shared<ParkingSpot>(id++, SpotType::Motorcycle));
        for (int i = 0; i < truckSpots; ++i)
            spots.push_back(make_shared<ParkingSpot>(id++, SpotType::Truck));
    }

    shared_ptr<ParkingSpot> findAvailableSpot(VehicleType vtype) {
        for (auto& spot : spots) {
            if (spot->isCompatible(vtype) && spot->isAvailable())
                return spot;
        }
        return nullptr;
    }

    void displayAvailableSpots() {
        map<SpotType, int> count;
        for (auto& spot : spots) {
            if (spot->isAvailable())
                count[spot->getType()]++;
        }

        cout << "Level " << levelNumber << " availability: ";
        for (auto& [type, c] : count) {
            cout << "[ " << static_cast<int>(type) << ": " << c << " ] ";
        }
        cout << "\n";
    }

    int getLevelNumber() const { return levelNumber; }

    const vector<shared_ptr<ParkingSpot>>& getSpots() const {
        return spots;
    }
};

// ---------- Parking Ticket ----------
class ParkingTicket {
    static int globalId;
    int ticketId;
    string licensePlate;
    int spotId;
    int level;
    time_t entryTime;

public:
    ParkingTicket(string plate, int sid, int lvl)
        : licensePlate(plate), spotId(sid), level(lvl) {
        ticketId = ++globalId;
        entryTime = time(nullptr);
    }

    void print() const {
        cout << "Ticket #" << ticketId
             << " | Plate: " << licensePlate
             << " | Spot: " << spotId
             << " | Level: " << level
             << " | Time: " << ctime(&entryTime);
    }

    int getLevel() const { return level; }
    int getSpotId() const { return spotId; }
};

int ParkingTicket::globalId = 0;

// ---------- Parking Lot ----------
class ParkingLot {
    vector<shared_ptr<Level>> levels;

public:
    ParkingLot(int numLevels, int carSpots, int motorcycleSpots, int truckSpots) {
        for (int i = 0; i < numLevels; ++i) {
            levels.push_back(make_shared<Level>(i, carSpots, motorcycleSpots, truckSpots));
        }
    }

    shared_ptr<ParkingTicket> parkVehicle(shared_ptr<Vehicle> v) {
        for (auto& level : levels) {
            auto spot = level->findAvailableSpot(v->getType());
            if (spot && spot->assignVehicle(v)) {
                return make_shared<ParkingTicket>(v->getPlate(), spot->getId(), level->getLevelNumber());
            }
        }
        cout << "No available spot for vehicle: " << v->getPlate() << "\n";
        return nullptr;
    }

    void unparkVehicle(shared_ptr<ParkingTicket> ticket) {
        auto level = levels[ticket->getLevel()];
        for (auto& spot : level->getSpots()) {
            if (spot->getId() == ticket->getSpotId()) {
                spot->removeVehicle();
                cout << "Vehicle unparked from Level " << ticket->getLevel()
                     << ", Spot " << ticket->getSpotId() << "\n";
                return;
            }
        }
        cout << "Invalid ticket\n";
    }

    void displayAvailability() {
        for (auto& level : levels) {
            level->displayAvailableSpots();
        }
    }
};

// ---------- Main ----------
int main() {
    ParkingLot lot(3, 10, 5, 2); // 3 levels, 10 car spots, 5 bike spots, 2 truck spots each

    auto car = make_shared<Car>("MH12AB1234");
    auto bike = make_shared<Motorcycle>("KA01XY9999");
    auto truck = make_shared<Truck>("DL88ZZ2222");

    auto carTicket = lot.parkVehicle(car);
    auto bikeTicket = lot.parkVehicle(bike);
    auto truckTicket = lot.parkVehicle(truck);

    if (carTicket) carTicket->print();
    if (bikeTicket) bikeTicket->print();
    if (truckTicket) truckTicket->print();

    lot.displayAvailability();

    lot.unparkVehicle(carTicket);

    lot.displayAvailability();

    return 0;
}
