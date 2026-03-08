import java.util.*;

/* -------- Vehicle Types -------- */
enum VehicleType {
	MOTORCYCLE,
	CAR,
	TRUCK
}

/* -------- Vehicle -------- */
abstract class Vehicle {
	String number;
	VehicleType type;

	Vehicle(String number, VehicleType type) {
		this.number = number;
		this.type = type;
	}
}

class Car extends Vehicle {
	Car(String number) {
		super(number, VehicleType.CAR);
	}
}

class Motorcycle extends Vehicle {
	Motorcycle(String number) {
		super(number, VehicleType.MOTORCYCLE);
	}
}

class Truck extends Vehicle {
	Truck(String number) {
		super(number, VehicleType.TRUCK);
	}
}

/* -------- Parking Spot -------- */
class ParkingSpot {

	String spotId;
	VehicleType supportedType;
	boolean occupied = false;

	ParkingSpot(String spotId, VehicleType type) {
		this.spotId = spotId;
		this.supportedType = type;
	}

	boolean canFit(Vehicle vehicle) {
		return !occupied && vehicle.type == supportedType;
	}

	void park() {
		occupied = true;
	}

	void leave() {
		occupied = false;
	}
}

/* -------- Parking Level -------- */
class ParkingLevel {

	int levelId;
	Map<VehicleType, Queue<ParkingSpot>> freeSpots = new HashMap<>();

	ParkingLevel(int levelId) {
		this.levelId = levelId;

		for (VehicleType type : VehicleType.values()) {
			freeSpots.put(type, new LinkedList<>());
		}
	}

	void addSpot(ParkingSpot spot) {
		freeSpots.get(spot.supportedType).offer(spot);
	}

	ParkingSpot assignSpot(Vehicle vehicle) {

		Queue<ParkingSpot> spots = freeSpots.get(vehicle.type);

		if (spots.isEmpty())
			return null;

		ParkingSpot spot = spots.poll();
		spot.park();
		return spot;
	}

	void releaseSpot(ParkingSpot spot) {
		spot.leave();
		freeSpots.get(spot.supportedType).offer(spot);
	}

	int getAvailability(VehicleType type) {
		return freeSpots.get(type).size();
	}
}

/* -------- Parking Lot -------- */
class ParkingLot {

	List<ParkingLevel> levels = new ArrayList<>();
	Map<String, ParkingSpot> activeVehicles = new HashMap<>();

	void addLevel(ParkingLevel level) {
		levels.add(level);
	}

	ParkingSpot parkVehicle(Vehicle vehicle) {

		for (ParkingLevel level : levels) {

			ParkingSpot spot = level.assignSpot(vehicle);

			if (spot != null) {
				activeVehicles.put(vehicle.number, spot);
				return spot;
			}
		}

		return null;
	}

	void unparkVehicle(String vehicleNumber) {

		ParkingSpot spot = activeVehicles.remove(vehicleNumber);

		if (spot == null)
			return;

		for (ParkingLevel level : levels) {
			level.releaseSpot(spot);
		}
	}

	void showAvailability() {

		System.out.println("\nParking Availability");

		for (ParkingLevel level : levels) {

			System.out.println("Level " + level.levelId);

			for (VehicleType type : VehicleType.values()) {

				System.out.println(
				    type + " spots: "
				    + level.getAvailability(type)
				);
			}
		}
	}
}

public class Main {

	public static void main(String[] args) {

		ParkingLot lot = new ParkingLot();

		ParkingLevel level1 = new ParkingLevel(1);
		level1.addSpot(new ParkingSpot("L1-C1", VehicleType.CAR));
		level1.addSpot(new ParkingSpot("L1-C2", VehicleType.CAR));
		level1.addSpot(new ParkingSpot("L1-M1", VehicleType.MOTORCYCLE));
		level1.addSpot(new ParkingSpot("L1-T1", VehicleType.TRUCK));

		ParkingLevel level2 = new ParkingLevel(2);
		level2.addSpot(new ParkingSpot("L2-C1", VehicleType.CAR));
		level2.addSpot(new ParkingSpot("L2-M1", VehicleType.MOTORCYCLE));

		lot.addLevel(level1);
		lot.addLevel(level2);

		Vehicle car = new Car("CAR-101");
		Vehicle bike = new Motorcycle("BIKE-201");
		Vehicle truck = new Truck("TRUCK-301");

		ParkingSpot s1 = lot.parkVehicle(car);
		ParkingSpot s2 = lot.parkVehicle(bike);
		ParkingSpot s3 = lot.parkVehicle(truck);

		System.out.println("Car parked at: " + (s1 != null ? s1.spotId : "No spot"));
		System.out.println("Bike parked at: " + (s2 != null ? s2.spotId : "No spot"));
		System.out.println("Truck parked at: " + (s3 != null ? s3.spotId : "No spot"));

		lot.showAvailability();

		System.out.println("\nUnparking CAR-101");
		lot.unparkVehicle("CAR-101");

		lot.showAvailability();
	}
}