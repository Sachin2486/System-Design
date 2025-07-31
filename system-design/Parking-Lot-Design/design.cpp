#include <bits/stdc++.h>
#include <iostream>

using namespace std;

using Clock = chrono::system_clock;
using TimePoint = chrono::time_point<Clock>;

// ---------- ENUMS ----------
enum class VehicleType {
    Motorcycle,
    Car,
    Bus
};

enum class SlotType { 
    MotorcycleSlot, 
    CarSlot,
    BusSlot
};

// ---------- VEHICLE ----------
class Vehicle {
    public:
    VehicleType type;
    string number;
    TimePoint entryTime;
    
    Vehicle(VehicleType type, const string& number)
      : type(type), number(number), entryTime(Clock::now()) {}
    
};

// ---------- SLOT ----------
class Slot {
    public:
    SlotType type;
    bool occupied;
    shared_ptr<Vehicle> vehicle;
    int id;
    
    Slot(SlotType type, int id) : type(type), occupied(false), id(id) {}
    
    bool canFit(VehicleType vType) {
        if(type == SlotType:: MotorcycleSlot) return vType == VehicleType:: Motorcycle;
        if (type == SlotType::CarSlot) return vType == VehicleType::Car || vType == VehicleType::Motorcycle;
        return true;
    }
    
    void park(shared_ptr<Vehicle> v) {
        occupied = true;
        vehicle = v;
    }
    
    void remove() {
        occupied = false;
        vehicle = nullptr;
    }
};

// ---------- LEVEL ----------
class Level {
public:
    int id;
    vector<shared_ptr<Slot>> slots;

    Level(int id) : id(id) {}

    void addSlot(SlotType type) {
        slots.push_back(make_shared<Slot>(type, slots.size()));
    }

    bool hasFreeSlotFor(VehicleType vType) {
        for (auto& slot : slots) {
            if (!slot->occupied && slot->canFit(vType)) return true;
        }
        return false;
    }

    shared_ptr<Slot> assignSlot(shared_ptr<Vehicle> v) {
        for (auto& slot : slots) {
            if (!slot->occupied && slot->canFit(v->type)) {
                slot->park(v);
                return slot;
            }
        }
        return nullptr;
    }

    void removeSlot(int slotId) {
        if (slotId >= 0 && slotId < slots.size()) {
            if (!slots[slotId]->occupied)
                slots.erase(slots.begin() + slotId);
        }
    }

    void status() {
        int free = 0, occ = 0;
        for (auto& slot : slots) {
            if (slot->occupied) occ++;
            else free++;
        }
        cout << "Level " << id << " => Free: " << free << ", Occupied: " << occ << endl;
    }
};

// ---------- PARKING LOT ----------
class ParkingLot {
    vector<shared_ptr<Level>> levels;
    unordered_map<string, shared_ptr<Slot>> parkedVehicles;

public:
    void addLevel() {
        levels.push_back(make_shared<Level>(levels.size()));
    }

    void removeLevel(int id) {
        if (id >= 0 && id < levels.size()) {
            levels.erase(levels.begin() + id);
        }
    }

    void addSlotToLevel(int levelId, SlotType type) {
        if (levelId >= 0 && levelId < levels.size()) {
            levels[levelId]->addSlot(type);
        }
    }

    bool parkVehicle(shared_ptr<Vehicle> v) {
        for (auto& level : levels) {
            if (level->hasFreeSlotFor(v->type)) {
                auto slot = level->assignSlot(v);
                if (slot) {
                    parkedVehicles[v->number] = slot;
                    cout << "Vehicle " << v->number << " parked at Level " << level->id << ", Slot " << slot->id << endl;
                    return true;
                }
            }
        }
        cout << "No available slot for vehicle: " << v->number << endl;
        return false;
    }

    void exitVehicle(const string& number) {
        if (parkedVehicles.find(number) != parkedVehicles.end()) {
            auto slot = parkedVehicles[number];
            auto vehicle = slot->vehicle;
            auto duration = chrono::duration_cast<chrono::hours>(Clock::now() - vehicle->entryTime).count();
            if (duration == 0) duration = 1;

            int fee = 0;
            switch (vehicle->type) {
                case VehicleType::Motorcycle: fee = duration * 1; break;
                case VehicleType::Car: fee = duration * 2; break;
                case VehicleType::Bus: fee = duration * 5; break;
            }

            slot->remove();
            parkedVehicles.erase(number);
            cout << "Vehicle " << number << " exited. Fee: $" << fee << " for " << duration << " hour(s).\n";
        } else {
            cout << "Vehicle not found.\n";
        }
    }

    void viewStatus() {
        for (auto& level : levels) {
            level->status();
        }
    }
};

int main() {
    ParkingLot lot;

    // Setup
    lot.addLevel();
    lot.addLevel();

    // Add slots
    lot.addSlotToLevel(0, SlotType::MotorcycleSlot);
    lot.addSlotToLevel(0, SlotType::CarSlot);
    lot.addSlotToLevel(0, SlotType::BusSlot);
    lot.addSlotToLevel(1, SlotType::CarSlot);

    // Vehicles
    auto v1 = make_shared<Vehicle>(VehicleType::Motorcycle, "M123");
    auto v2 = make_shared<Vehicle>(VehicleType::Car, "C456");
    auto v3 = make_shared<Vehicle>(VehicleType::Bus, "B789");

    // Park vehicles
    lot.parkVehicle(v1);
    lot.parkVehicle(v2);
    lot.parkVehicle(v3);

    // View status
    lot.viewStatus();

    // Simulate exit
    std::this_thread::sleep_for(std::chrono::seconds(2)); // Simulate time
    lot.exitVehicle("C456");

    lot.viewStatus();

    return 0;
}
