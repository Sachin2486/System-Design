#include <bits/stdc++.h>
#include <iostream>

using namespace std;

class Ride {
    public:
    
    string userId;
    int origin;
    int destination;
    int seats;
    
    Ride(string id, int o, int d, int s) : userId(id), origin(o), destination(d), seats(s) {}
};

class Driver : public Ride {
public:
    Driver(string id, int o, int d, int s) : Ride(id, o, d, s) {}
};

class Rider : public Ride {
public:
    Rider(string id, int o, int d, int s) : Ride(id, o, d, s) {}
};

class RideSharingSystem {
    private:
    vector<Driver> drivers;
    vector<Rider> riders;
    
    int getOverlap(const Rider& rider, const Driver& driver) {
        int overlapStart = max(rider.origin, driver.origin);
        int overlapEnd = min(rider.destination, driver.destination);
        return max(0, overlapEnd - overlapStart);
    }
    
    public:
    void addDriver(string userId, int origin, int destination, int seats) {
         drivers.emplace_back(userId, origin, destination, seats);
         cout << "Driver" << userId << "added.\n";
    }
    
    void addRider(string userId, int origin, int destination, int seats) {
        riders.emplace_back(userId, origin, destination, seats);
        cout << "Rider " << userId << " requesting ride...\n";

        int maxOverlap = -1;
        int selectedDriverIndex = -1;

        for (int i = 0; i < drivers.size(); ++i) {
            Driver& driver = drivers[i];

            if (driver.seats >= seats && 
                driver.origin <= origin && 
                driver.destination >= destination) {

                int overlap = getOverlap(riders.back(), driver);
                if (overlap > maxOverlap) {
                    maxOverlap = overlap;
                    selectedDriverIndex = i;
                }
            }
        }
        
    if (selectedDriverIndex != -1) {
            Driver& selectedDriver = drivers[selectedDriverIndex];
            selectedDriver.seats -= seats;
            cout << "Rider " << userId << " matched with Driver " 
                 << selectedDriver.userId << " with overlap: " << maxOverlap << "\n";
        } else {
            cout << "No suitable driver found for Rider " << userId << ".\n";
        }
    }

    void showAvailableDrivers() {
        cout << "\nAvailable Drivers:\n";
        for (auto& d : drivers) {
            cout << "Driver " << d.userId << ": " << d.origin << " -> " << d.destination
                 << ", seats: " << d.seats << "\n";
        }
    }
};

int main() {
    RideSharingSystem system;

    system.addDriver("D1", 0, 10, 3);
    system.addDriver("D2", 2, 8, 2);
    system.addDriver("D3", 1, 5, 1);

    system.showAvailableDrivers();

    system.addRider("R1", 3, 7, 1);  // Should match with D2 (max overlap)
    system.addRider("R2", 2, 4, 1);  // Should match with D3

    system.showAvailableDrivers();

    system.addRider("R3", 0, 9, 2);  // Should match with D1
    system.addRider("R4", 1, 9, 2);  // Should fail (no seats left)

    return 0;
}


