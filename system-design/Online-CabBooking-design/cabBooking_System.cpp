#include <iostream>
#include <string>
#include <vector>
#include <cmath>
#include <map>

using namespace std;

enum RideType {
	REGULAR,
	PREMIUM
};

class Passenger {
public:
	string name;
	string pickupLocation;
	string destination;
	RideType rideType;

	Passenger(string n, string pickup, string dest, RideType type)
		: name(n), pickupLocation(pickup), destination(dest), rideType(type) {}
};

class Driver {
public:
	string name;
	string location;
	bool available;

	Driver(string name, string location)
		: name(name), location(location), available(true) {}

	void acceptRide() {
		available = false;
	}

	void completeRide() {
		available = true;
	}
};

class RideRequest {
public:
	Passenger passenger;
	Driver* driver;
	double distance;
	double time;
	double fare;

	RideRequest(Passenger p, double dist, double t)
		: passenger(p), driver(nullptr), distance(dist), time(t), fare(0) {}

	void assignDriver(Driver* d) {
		driver = d;
		driver->acceptRide();
		calculateFare();
	}

	void calculateFare() {
		double baseFare = (passenger.rideType == REGULAR) ? 5.0 : 10.0;
		double ratePerKm = (passenger.rideType == REGULAR) ? 1.0 : 2.0;
		double ratePerMin = (passenger.rideType == REGULAR) ? 0.5 : 1.0;

		fare = baseFare + (ratePerKm * distance) + (ratePerMin * time);
	}

	void completeRide() {
		if (driver) {
			driver->completeRide();
		}
	}
};

class Payment {
public:
	static void processPayment(Passenger passenger, Driver driver, double amount) {
		cout << "Processing payment of $" << amount << " from " << passenger.name << " to " << driver.name << endl;
	}
};

class RideSharingSystem {
public:
	vector<Driver*> drivers;
	vector<RideRequest*> rideRequests;

	void addDriver(Driver* driver) {
		drivers.push_back(driver);
	}

	void addRideRequest(RideRequest* rideRequest) {
		rideRequests.push_back(rideRequest);
		matchDriver(rideRequest);
	}

	void matchDriver(RideRequest* rideRequest) {
		Driver* closestDriver = nullptr;
		double closestDistance = INFINITY;

		for (auto& driver : drivers) {
			if (driver->available) {
				double distance = calculateDistance(driver->location, rideRequest->passenger.pickupLocation);
				if (distance < closestDistance) {
					closestDistance = distance;
					closestDriver = driver;
				}
			}
		}

		if (closestDriver) {
			rideRequest->assignDriver(closestDriver);
			cout << "Driver " << closestDriver->name << " assigned to passenger " << rideRequest->passenger.name << endl;
		} else {
			cout << "No available drivers for passenger " << rideRequest->passenger.name << endl;
		}
	}

	void completeRide(RideRequest* rideRequest) {
		rideRequest->completeRide();
		Payment::processPayment(rideRequest->passenger, *(rideRequest->driver), rideRequest->fare);
	}

private:
	double calculateDistance(string loc1, string loc2) {
		// Simplified distance calculation for example testing
		return abs(loc1[0] - loc2[0]) + abs(loc1[1] - loc2[1]);
	}
};

int main() {
	Driver driver1("Alice", "A1");
	Driver driver2("Bob", "B2");
	Driver driver3("Charlie", "C3");

	// Create ride-sharing system
	RideSharingSystem system;
	system.addDriver(&driver1);
	system.addDriver(&driver2);
	system.addDriver(&driver3);

	// Create some dummy passengers and ride requests
	Passenger passenger1("John", "A1", "D4", REGULAR);
	RideRequest request1(passenger1, 5, 10);
	system.addRideRequest(&request1);

	Passenger passenger2("Jane", "B2", "E5", PREMIUM);
	RideRequest request2(passenger2, 7, 12);
	system.addRideRequest(&request2);

	// Complete rides response after testing
	system.completeRide(&request1);
	system.completeRide(&request2);

	return 0;
}
