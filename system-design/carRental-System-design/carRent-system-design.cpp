#include<bits/stdc++.h>

using namespace std;

enum class CarType {
	SUV,
	SEDAN,
	HATCHBACK
};

string carTypeToString(CarType type) {
	switch (type) {
	case CarType::SUV:
		return "SUV";
	case CarType::SEDAN:
		return "Sedan";
	case CarType::HATCHBACK:
		return "Hatchback";
	default:
		return "Unknown";
	}
}

class Car {
public:
	string licensePlate, make, model;
	int year;
	CarType type;
	double pricePerDay;
	map<string, bool> availability;

	Car(string lp, string mk, string mdl, int yr, CarType ct, double price) :
		licensePlate(lp), make(mk), model(mdl), year(yr), type(ct), pricePerDay(price) {}

	bool isAvailable(const string& startDate, const string& endDate) {
		int start = stoi(startDate);
		int end = stoi(endDate);

		for (const auto& entry : availability) {
			int date = stoi(entry.first);
			bool booked = entry.second;
			if (date >= start && date <= end && booked)
				return false;
		}
		return true;
	}

	void reserveDates(const string& startDate, const string& endDate) {
		int start = stoi(startDate);
		int end = stoi(endDate);

		for (int d = start; d <= end; ++d) {
			availability[to_string(d)] = true;
		}
	}

	void cancelDates(const string& startDate, const string& endDate) {
		int start = stoi(startDate);
		int end = stoi(endDate);

		for (int d = start; d <= end; ++d) {
			availability[to_string(d)] = false;
		}
	}

	void display() {
		cout << make << " " << model << " (" << year << ")"
		     << " [" << licensePlate << "] - "
		     << carTypeToString(type) << " b9" << pricePerDay << "/day\n";
	}
};

class Customer {
public:
	string name, contact, licenseNumber;
	Customer(string n, string c, string l) : name(n), contact(c), licenseNumber(l) {}
};

class Reservation {
public:
	shared_ptr<Car> car;
	shared_ptr<Customer> customer;
	string startDate, endDate;
	double totalPrice;

	Reservation(shared_ptr<Car> c, shared_ptr<Customer> cust,
	            string start, string end, double price)
		: car(c), customer(cust), startDate(start), endDate(end), totalPrice(price) {}

	void display() {
		cout << "Reservation for " << customer->name << " ("
		     << customer->licenseNumber << ")\nCar: "
		     << car->make << " " << car->model << ", Dates: " << startDate
		     << " to " << endDate << ", Total b9" << totalPrice << "\n";
	}
};

class RentalSystem {
	vector<shared_ptr<Car>> cars;
	vector<shared_ptr<Customer>> customers;
	vector<shared_ptr<Reservation>> reservations;

public:
	void addCar(shared_ptr<Car> car) {
		cars.push_back(car);
	}

	void addCustomer(shared_ptr<Customer> cust) {
		customers.push_back(cust);
	}

	void searchCars(CarType type, double minPrice, double maxPrice, string startDate, string endDate) {
		cout << "\nAvailable Cars:\n";
		for (auto& car : cars) {
			if (car->type == type &&
			        car->pricePerDay >= minPrice &&
			        car->pricePerDay <= maxPrice &&
			        car->isAvailable(startDate, endDate)) {
				car->display();
			}
		}
	}

	void createReservation(string licensePlate, string customerName, string startDate, string endDate) {
		auto car = getCarByLicense(licensePlate);
		auto cust = getCustomerByName(customerName);
		if (!car || !cust) {
			cout << "Car or Customer not found.\n";
			return;
		}

		if (!car->isAvailable(startDate, endDate)) {
			cout << "Car not available for selected dates.\n";
			return;
		}

		int days = stoi(endDate) - stoi(startDate) + 1;
		double total = days * car->pricePerDay;

		auto res = make_shared<Reservation>(car, cust, startDate, endDate, total);
		reservations.push_back(res);
		car->reserveDates(startDate, endDate);
		cout << "Booking successful! Total b9" << total << "\n";
		res->display();
	}

	void cancelReservation(string customerName, string licensePlate, string startDate, string endDate) {
		for (auto it = reservations.begin(); it != reservations.end(); ++it) {
			if ((*it)->customer->name == customerName &&
			        (*it)->car->licensePlate == licensePlate &&
			        (*it)->startDate == startDate &&
			        (*it)->endDate == endDate) {
				(*it)->car->cancelDates(startDate, endDate);
				reservations.erase(it);
				cout << "Reservation canceled successfully.\n";
				return;
			}
		}
		cout << "No matching reservation found.\n";
	}

private:
	shared_ptr<Car> getCarByLicense(const string& plate) {
		for (auto& car : cars)
			if (car->licensePlate == plate) return car;
		return nullptr;
	}

	shared_ptr<Customer> getCustomerByName(const string& name) {
		for (auto& c : customers)
			if (c->name == name) return c;
		return nullptr;
	}
};

int main() {
	RentalSystem system;

	auto c1 = make_shared<Car>("MH01AB1234", "Toyota", "Camry", 2021, CarType::SEDAN, 2500);
	auto c2 = make_shared<Car>("MH01CD5678", "Hyundai", "Creta", 2022, CarType::SUV, 3200);
	auto c3 = make_shared<Car>("MH01EF9999", "Tata", "Punch", 2023, CarType::HATCHBACK, 1800);

	system.addCar(c1);
	system.addCar(c2);
	system.addCar(c3);

	auto cust1 = make_shared<Customer>("Sachin Tiwari", "9999999999", "DL12345678");
	system.addCustomer(cust1);

	system.searchCars(CarType::SUV, 2000, 3500, "20240605", "20240607");
	system.createReservation("MH01CD5678", "Sachin Tiwari", "20240605", "20240607");

	system.cancelReservation("Sachin Tiwari", "MH01CD5678", "20240605", "20240607");

	return 0;
}
