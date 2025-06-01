#include <bits/stdc++.h>
using namespace std;

enum class VehicleType {
    CAR,
    MOTORCYCLE,
    TRUCK
};

enum class SpotStatus {
    AVAILABLE,
    OCCUPIED
};

class Vehicle {
private:
    string licensePlate;
    VehicleType type;
    time_t entryTime;

public:
    Vehicle(const string& plate, VehicleType vehicleType)
        : licensePlate(plate), type(vehicleType) {
        entryTime = time(nullptr);
    }

    string getLicensePlate() const {
        return licensePlate;
    }

    VehicleType getType() const {
        return type;
    }

    time_t getEntryTime() const {
        return entryTime;
    }

    string getTypeString() const {
        switch (type) {
            case VehicleType::CAR: return "Car";
            case VehicleType::MOTORCYCLE: return "Motorcycle";
            case VehicleType::TRUCK: return "Truck";
            default: return "Unknown";
        }
    }
};

class ParkingSpot {
private:
    int spotId;
    VehicleType allowedType;
    SpotStatus status;
    shared_ptr<Vehicle> parkedVehiclePtr;

public:
    ParkingSpot(int id, VehicleType type)
        : spotId(id), allowedType(type), status(SpotStatus::AVAILABLE), parkedVehiclePtr(nullptr) {}

    bool canPark(VehicleType vehicleType) const {
        return status == SpotStatus::AVAILABLE && allowedType == vehicleType;
    }

    bool parkVehicle(shared_ptr<Vehicle> vehicle) {
        if (canPark(vehicle->getType())) {
            parkedVehiclePtr = vehicle;
            status = SpotStatus::OCCUPIED;
            return true;
        }
        return false;
    }

    shared_ptr<Vehicle> removeVehicle() {
        if (status == SpotStatus::OCCUPIED) {
            auto vehicle = parkedVehiclePtr;
            parkedVehiclePtr = nullptr;
            status = SpotStatus::AVAILABLE;
            return vehicle;
        }
        return nullptr;
    }

    int getSpotId() const {
        return spotId;
    }

    VehicleType getAllowedType() const {
        return allowedType;
    }

    SpotStatus getStatus() const {
        return status;
    }

    shared_ptr<Vehicle> getParkedVehicle() const {
        return parkedVehiclePtr;
    }

    bool isAvailable() const {
        return status == SpotStatus::AVAILABLE;
    }
};

class Level {
private:
    int levelNumber;
    vector<shared_ptr<ParkingSpot>> spots;
    unordered_map<VehicleType, int> availableSpots;

public:
    Level(int level) : levelNumber(level) {
        availableSpots[VehicleType::CAR] = 0;
        availableSpots[VehicleType::MOTORCYCLE] = 0;
        availableSpots[VehicleType::TRUCK] = 0;
    }

    void addParkingSpot(VehicleType type, int count = 1) {
        for (int i = 0; i < count; i++) {
            int spotId = spots.size() + 1;
            spots.push_back(make_shared<ParkingSpot>(spotId, type));
            availableSpots[type]++;
        }
    }

    shared_ptr<ParkingSpot> findAvailableSpot(VehicleType type) {
        for (auto& spot : spots) {
            if (spot->canPark(type)) {
                return spot;
            }
        }
        return nullptr;
    }

    bool parkVehicle(shared_ptr<Vehicle> vehicle) {
        auto spot = findAvailableSpot(vehicle->getType());
        if (spot && spot->parkVehicle(vehicle)) {
            availableSpots[vehicle->getType()]--;
            return true;
        }
        return false;
    }

    bool removeVehicle(const string& licensePlate) {
        for (auto& spot : spots) {
            if (spot->getStatus() == SpotStatus::OCCUPIED &&
                spot->getParkedVehicle()->getLicensePlate() == licensePlate) {
                auto vehicle = spot->removeVehicle();
                if (vehicle) {
                    availableSpots[vehicle->getType()]++;
                    return true;
                }
            }
        }
        return false;
    }

    int getLevelNumber() const {
        return levelNumber;
    }

    int getAvailableSpots(VehicleType type) const {
        auto it = availableSpots.find(type);
        return (it != availableSpots.end()) ? it->second : 0;
    }

    int getTotalSpots() const {
        return spots.size();
    }

    void displayStatus() const {
        cout << "Level " << levelNumber << " Status:\n";
        cout << "  Cars: " << getAvailableSpots(VehicleType::CAR) << " available\n";
        cout << "  Motorcycles: " << getAvailableSpots(VehicleType::MOTORCYCLE) << " available\n";
        cout << "  Trucks: " << getAvailableSpots(VehicleType::TRUCK) << " available\n";
    }
};

class ParkingLot {
private:
    vector<shared_ptr<Level>> levels;
    unordered_map<string, pair<int, int>> vehicleLocations; // licensePlate -> (level, spotId)

public:
    void addLevel(int levelNumber) {
        levels.push_back(make_shared<Level>(levelNumber));
    }

    void addParkingSpotsToLevel(int levelNumber, VehicleType type, int count) {
        if (levelNumber > 0 && levelNumber <= levels.size()) {
            levels[levelNumber - 1]->addParkingSpot(type, count);
        }
    }

    bool parkVehicle(shared_ptr<Vehicle> vehicle) {
        for (auto& level : levels) {
            if (level->parkVehicle(vehicle)) {
                vehicleLocations[vehicle->getLicensePlate()] = {level->getLevelNumber(), -1};
                cout << "Vehicle " << vehicle->getLicensePlate()
                     << " (" << vehicle->getTypeString() << ") parked on Level "
                     << level->getLevelNumber() << endl;
                return true;
            }
        }
        cout << "No available spot for vehicle " << vehicle->getLicensePlate()
             << " (" << vehicle->getTypeString() << ")" << endl;
        return false;
    }

    bool removeVehicle(const string& licensePlate) {
        auto it = vehicleLocations.find(licensePlate);
        if (it != vehicleLocations.end()) {
            int levelNum = it->second.first;
            if (levelNum > 0 && levelNum <= levels.size()) {
                if (levels[levelNum - 1]->removeVehicle(licensePlate)) {
                    vehicleLocations.erase(it);
                    cout << "Vehicle " << licensePlate << " removed from Level "
                         << levelNum << endl;
                    return true;
                }
            }
        }
        cout << "Vehicle " << licensePlate << " not found in parking lot" << endl;
        return false;
    }

    void displayRealTimeStatus() const {
        cout << "\n=== PARKING LOT REAL-TIME STATUS ===" << endl;
        for (const auto& level : levels) {
            level->displayStatus();
        }
        cout << "==============================\n" << endl;
    }

    void displayTotalAvailability() const {
        int totalCars = 0, totalMotorcycles = 0, totalTrucks = 0;

        for (const auto& level : levels) {
            totalCars += level->getAvailableSpots(VehicleType::CAR);
            totalMotorcycles += level->getAvailableSpots(VehicleType::MOTORCYCLE);
            totalTrucks += level->getAvailableSpots(VehicleType::TRUCK);
        }

        cout << "TOTAL AVAILABLE SPOTS:" << endl;
        cout << "Cars: " << totalCars << endl;
        cout << "Motorcycles: " << totalMotorcycles << endl;
        cout << "Trucks: " << totalTrucks << endl;
    }
};

void demonstrateParkingLot() {
    ParkingLot parkingLot;

    parkingLot.addLevel(1);
    parkingLot.addLevel(2);
    parkingLot.addLevel(3);

    parkingLot.addParkingSpotsToLevel(1, VehicleType::CAR, 20);
    parkingLot.addParkingSpotsToLevel(1, VehicleType::MOTORCYCLE, 30);
    parkingLot.addParkingSpotsToLevel(1, VehicleType::TRUCK, 5);

    parkingLot.addParkingSpotsToLevel(2, VehicleType::CAR, 40);
    parkingLot.addParkingSpotsToLevel(2, VehicleType::MOTORCYCLE, 10);

    parkingLot.addParkingSpotsToLevel(3, VehicleType::CAR, 25);
    parkingLot.addParkingSpotsToLevel(3, VehicleType::TRUCK, 15);

    cout << "Initial Parking Lot Status:" << endl;
    parkingLot.displayRealTimeStatus();

    auto car1 = make_shared<Vehicle>("ABC123", VehicleType::CAR);
    auto car2 = make_shared<Vehicle>("XYZ789", VehicleType::CAR);
    auto motorcycle1 = make_shared<Vehicle>("BIKE001", VehicleType::MOTORCYCLE);
    auto truck1 = make_shared<Vehicle>("TRUCK001", VehicleType::TRUCK);

    cout << "Parking vehicles..." << endl;
    parkingLot.parkVehicle(car1);
    parkingLot.parkVehicle(motorcycle1);
    parkingLot.parkVehicle(truck1);
    parkingLot.parkVehicle(car2);

    cout << "\nAfter parking vehicles:" << endl;
    parkingLot.displayRealTimeStatus();

    cout << "Removing vehicle ABC123..." << endl;
    parkingLot.removeVehicle("ABC123");

    cout << "\nFinal status:" << endl;
    parkingLot.displayRealTimeStatus();
    parkingLot.displayTotalAvailability();
}

int main() {
    cout << "Multi-Level Parking Lot System Demo" << endl;
    cout << "====================================" << endl;

    demonstrateParkingLot();

    return 0;
}
