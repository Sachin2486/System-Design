// class Car --> represents a car with details (make, model, year, plate, rental price, type).
// class customer --> customer info (name, contact, driver license).
// class Reservation connects a Customer with a Car for specific dates.
// class Payment  handles payment for reservations.
// class CarRentalSystem  main system: browsing cars, searching, managing reservations.


import java.util.*;

class Car {
	private String model;
	private String make;
	private int year;
	private String licensePlate;
	private double pricePerDay ;
	private String type;
	private boolean available;

	public Car(String make,String model, int year, String licensePlate, double pricePerDay, String type) {
		this.make = make;
		this.model = model;
		this.year = year;
		this.licensePlate = licensePlate;
		this.pricePerDay = pricePerDay;
		this.type = type;
		this.available = true;
	}

	public String getLicensePlate() {
		return licensePlate;
	}

	public double getPricePerDay() {
		return pricePerDay;
	}

	public boolean isAvailable() {
		return available;
	}

	public String getType() {
		return type;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	@Override
	public String toString() {
		return year + " " + make + " " + model + " [" + licensePlate + "] - " +
		       type + " @ b9" + pricePerDay + "/day | " + (available ? "Available" : "Booked");
	}
}

class Customer {
	private String name;
	private String contact;
	private String driverLicense;

	public Customer(String name, String contact, String driverLicense) {
		this.name = name;
		this.contact = contact;
		this.driverLicense = driverLicense;
	}

	public String getName() {
		return name;
	}
	public String getDriverLicense() {
		return driverLicense;
	}
}

class Reservation {
	private static int counter = 1;
	private int reservationId;
	private Customer customer;
	private Car car;
	private Date startDate;
	private Date endDate;
	private boolean active;

	public Reservation(Customer customer, Car car, Date startDate, Date endDate) {
		this.reservationId = counter++;
		this.customer = customer;
		this.car = car;
		this.startDate = startDate;
		this.endDate = endDate;
		this.active = true;
		car.setAvailable(false); // Car is now reserved
	}

	public int getReservationId() {
		return reservationId;
	}

	public Car getCar() {
		return car;
	}

	public Customer getCustomer() {
		return customer;
	}

	public boolean isActive() {
		return active;
	}

	public void cancel() {
		this.active = false;
		car.setAvailable(true); //Release car
	}

	@Override
	public String toString() {
		return "Reservation #" + reservationId + " -> " + car + " for " + customer.getName() +
		       " [" + startDate + " to " + endDate + "] " + (active ? "Active" : "Cancelled");
	}
}

class Payment {
	private Reservation reservation;
	private double amount;
	private boolean paid;

	public Payment(Reservation reservation, double amount) {
		this.reservation = reservation;
		this.amount = amount;
		this.paid = false;
	}

	public void processPayment() {
		// Mocking payment processing
		this.paid = true;
		System.out.println("Payment of:" + amount + " processed for Reservation #" + reservation.getReservationId());
	}

	public boolean isPaid() {
		return paid;
	}
}

class CarRentalSystem {
	private List <Car> cars;
	private List<Reservation> reservations;

	public CarRentalSystem() {
		this.cars = new ArrayList<> ();
		this.reservations = new ArrayList<> ();
	}

	public void addCar(Car car) {
		cars.add(car);
	}

	public void browseCars() {
		System.out.println("Available Cars:");
		for(Car car : cars) {
			System.out.println(car);
		}
	}

	// Search by filters (type, price range, availability)
	public List<Car> searchCars(String type, double minPrice, double maxPrice) {
		List<Car> result = new ArrayList<>();
		for (Car car : cars) {
			if (car.isAvailable()
			        && car.getType().equalsIgnoreCase(type)
			        && car.getPricePerDay() >= minPrice
			        && car.getPricePerDay() <= maxPrice) {
				result.add(car);
			}
		}
		return result;
	}

	// Create reservation
	public Reservation reserveCar(Customer customer, Car car, Date start, Date end) {
		if (!car.isAvailable()) {
			System.out.println("Car " + car.getLicensePlate() + " is not available.");
			return null;
		}
		Reservation reservation = new Reservation(customer, car, start, end);
		reservations.add(reservation);

		long days = (end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
		double amount = days * car.getPricePerDay();
		Payment payment = new Payment(reservation, amount);
		payment.processPayment();

		return reservation;
	}

	// Cancel reservation
	public void cancelReservation(int reservationId) {
		for (Reservation res : reservations) {
			if (res.getReservationId() == reservationId && res.isActive()) {
				res.cancel();
				System.out.println("Reservation #" + reservationId + " cancelled.");
				return;
			}
		}
		System.out.println("Reservation not found or already cancelled.");
	}
}

public class CarRentalDemo {
	public static void main(String[] args) {
		CarRentalSystem system = new CarRentalSystem();

		// Add cars
		system.addCar(new Car("Toyota", "Corolla", 2020, "MH12AB1234", 2000, "Sedan"));
		system.addCar(new Car("Honda", "City", 2021, "MH14XY5678", 2500, "Sedan"));
		system.addCar(new Car("Mahindra", "Thar", 2022, "MH01ZZ1111", 3000, "SUV"));

		// Browse cars
		system.browseCars();

		// Customer
		Customer c1 = new Customer("Sachin", "9876543210", "DL12345");

		// Search available SUVs in price range
		List<Car> suvList = system.searchCars("SUV", 2000, 4000);
		System.out.println("\nSearch Results:");
		for (Car car : suvList) {
			System.out.println(car);
		}

		// Reserve a car
		Calendar cal = Calendar.getInstance();
		Date start = cal.getTime();
		cal.add(Calendar.DATE, 3);
		Date end = cal.getTime();

		Reservation r1 = system.reserveCar(c1, suvList.get(0), start, end);

		// Browse again
		System.out.println("\nAfter Reservation:");
		system.browseCars();

		// Cancel reservation
		if (r1 != null) {
			system.cancelReservation(r1.getReservationId());
		}

		// Final status
		System.out.println("\nFinal Car Status:");
		system.browseCars();
	}
}


