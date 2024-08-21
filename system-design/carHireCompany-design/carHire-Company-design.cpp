#include <iostream>
#include <string>
#include <vector>
#include <iomanip>

using namespace std;

class Car {
private:
	string registrationNumber;
	string make;
	string model;
	bool isBooked;
	string customerName;
	string bookingStartDate;
	string bookingEndDate;

public:
	Car(string regNumber, string carMake, string carModel) : registrationNumber(regNumber), make(carMake), model(carModel),
		isBooked(false), customerName(""), bookingStartDate(bookingStartDate),bookingEndDate(bookingEndDate) {}

	string getRegistrationNumber () const {
		return registrationNumber;
	}

	bool getBookingStatus() const {
		return isBooked;
	}

	void bookCar(string customer, string startDate, string endDate) {
		if(!isBooked) {
			customerName = customer;
			bookingStartDate = startDate;
			bookingEndDate = endDate;
			isBooked = true;
			cout<<"Car booked Successfully!"<<endl;
		}
		else {
			cout<<"Car is already booked"<<endl;
		}
	}

	void returnCar() {
		if(isBooked) {
			isBooked = false;
			customerName = "";
			bookingStartDate = "";
			bookingEndDate = "";
			cout<<"Car returned successfully!"<<endl;
		} else {
			cout<<"Car is not currently booked!"<<endl;
		}
	}

	void displayDetails() const {
		cout << "Registration Number: " << registrationNumber << endl;
		cout << "Make: " << make << endl;
		cout << "Model: " << model << endl;
		if(isBooked) {
			cout<<"Status : Booked"<<endl;
			cout<<"Customer"<<customerName<<endl;
			cout << "Booking Start Date: " << bookingStartDate << endl;
			cout << "Booking End Date: " << bookingEndDate << endl;
		} else {
			cout<<"Status available at depot!"<<endl;
		}
	}

};

class carHireSystem {
private:
	vector<Car> fleet;

public:
	void addCar(const Car &car) {
		fleet.push_back(car);
	}

	void displayAllCars() const {
		for(const auto& car : fleet) {
			car.displayDetails();
			cout << "------------------------------------" << endl;
		}
	}

	void bookCar(string regNumber, string customer,string startDate,string endDate) {
		for(auto &car : fleet) {
			if(car.getRegistrationNumber() == regNumber) {
				car.bookCar(customer,startDate,endDate);
				return;
			}
		}
		cout<<"Car not found!"<<endl;
	}

	void returnCar(string regNumber) {
		for(auto &car:fleet) {
			if(car.getRegistrationNumber() == regNumber) {
				car.returnCar();
				return;
			}
		}
		cout<<"Car not found"<<endl;
	}

	void checkCarStatus(string regNumber) {
		for(auto &car:fleet) {
			if(car.getRegistrationNumber() == regNumber) {
				car.displayDetails();
				return;
			}
		}
		cout<<"Car not found!"<<endl;
	}
};

int main()
{
	carHireSystem system;

	system.addCar(Car("AB123CD", "Toyota", "Corolla"));
	system.addCar(Car("EF456GH", "Honda", "Civic"));
	system.addCar(Car("IJ789KL", "Ford", "Focus"));

	system.displayAllCars();

	cout << "\nBooking car AB123CD for John Doe from 2024-08-22 to 2024-08-25..." << endl;
	system.bookCar("AB123CD", "John Doe", "2024-08-22", "2024-08-25");

	cout << "\nChecking the status of car AB123CD..." << endl;
	system.checkCarStatus("AB123CD");

	cout << "\nReturning car AB123CD..." << endl;
	system.returnCar("AB123CD");

	cout << "\nChecking the status of car AB123CD again..." << endl;
	system.checkCarStatus("AB123CD");

	return 0;
}