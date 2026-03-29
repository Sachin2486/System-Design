import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


enum VehicleType {
	CAR,
	MOTORCYCLE,
	TRUCK
}

abstract class Vehicle {
	String number;
	VehicleType type;

	Vehicle(String number,VehicleType type) {
		this.number = number;
		this.type = type;
	}
}

class Car extends Vehicle {
	Car(String number) {
		super(number, VehicleType.CAR);
	}
}

class MOTORCYCLE extends Vehicle {
	MOTORCYCLE(String number) {
		super(number, VehicleType.MOTORCYCLE);
	}
}

class TRUCK extends Vehicle {
	TRUCK(String number) {
		super(number, VehicleType.TRUCK);
	}
}

class ParkingSpot {
	String id;
	VehicleType type;
	boolean occupied = false;

	ParkingSpot(String id, VehicleType type) {
		this.id = id;
		this.type = type;
	}

	void park() {
		occupied = true;
	}

	void leave() {
		occupied = false;
	}
}

class ParkingLevel {
	int levelId;
	Map<VehicleType, Queue<ParkingSpot>> freeSpots = new HashMap<>();

	ParkingLevel(int id) {
		this.levelId = id;
		for(VehicleType type : VehicleType.values()) {
			freeSpots.put(type, new LinkedList<>());
		}
	}

	void addSpot(ParkingSpot spot) {
		freeSpots.get(spot.type).offer(spot);
	}

	ParkingSpot getSpot(Vehicle vehicle) {
		Queue<ParkingSpot> queue = freeSpots.get(vehicle.type);
		if (queue.isEmpty()) return null;

		ParkingSpot spot = queue.poll();
		spot.park();
		return spot;
	}

	void releaseSpot(ParkingSpot spot) {
		spot.leave();
		freeSpots.get(spot.type).offer(spot);
	}

	int getAvailable(VehicleType type) {
		return freeSpots.get(type).size();
	}
}

class ParkingTicket {
	String ticketId;
	String vehicleNumber;
	ParkingSpot spot;
	long entryTime;

	ParkingTicket(String id, String vehicleNumber, ParkingSpot spot) {
		this.ticketId = id;
		this.vehicleNumber = vehicleNumber;
		this.spot = spot;
		this.entryTime = System.currentTimeMillis();
	}
}

class ParkingLot {
	List<ParkingLevel> levels = new ArrayList<>();
	Map<String, ParkingTicket> activeTickets = new HashMap<>();
	ReentrantLock lock = new ReentrantLock();

	void addLevel(ParkingLevel level) {
		levels.add(level);
	}

	ParkingTicket parkVehicle(Vehicle vehicle) {
		lock.lock();
		try {
			for (ParkingLevel level : levels) {
				ParkingSpot spot = level.getSpot(vehicle);
				if (spot != null) {
					String ticketId = UUID.randomUUID().toString();
					ParkingTicket ticket =
					    new ParkingTicket(ticketId, vehicle.number, spot);
					activeTickets.put(vehicle.number, ticket);
					return ticket;
				}
			}
			return null;
		} finally {
			lock.unlock();
		}
	}

	void unparkVehicle(String vehicleNumber) {
		lock.lock();
		try {
			ParkingTicket ticket = activeTickets.remove(vehicleNumber);
			if (ticket == null) return;

			for (ParkingLevel level : levels) {
				level.releaseSpot(ticket.spot);
			}
		} finally {
			lock.unlock();
		}
	}

	void showAvailability() {
		System.out.println("\nParking Availability:");
		for (ParkingLevel level : levels) {
			System.out.println("Level " + level.levelId);
			for (VehicleType type : VehicleType.values()) {
				System.out.println(type + ": " +
				                   level.getAvailable(type));
			}
		}
	}
}

/* ================= Entry Gate ================= */
class EntryGate {
	ParkingLot lot;

	EntryGate(ParkingLot lot) {
		this.lot = lot;
	}

	ParkingTicket enter(Vehicle vehicle) {
		return lot.parkVehicle(vehicle);
	}
}

/* ================= Exit Gate ================= */
class ExitGate {
	ParkingLot lot;

	ExitGate(ParkingLot lot) {
		this.lot = lot;
	}

	void exit(String vehicleNumber) {
		lot.unparkVehicle(vehicleNumber);
	}
}

/* ================= MAIN ================= */
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

		EntryGate entry1 = new EntryGate(lot);
		ExitGate exit1 = new ExitGate(lot);

		Vehicle car = new Car("CAR-111");
		Vehicle bike = new MOTORCYCLE("BIKE-222");

		ParkingTicket t1 = entry1.enter(car);
		ParkingTicket t2 = entry1.enter(bike);

		System.out.println("Car parked at: " + t1.spot.id);
		System.out.println("Bike parked at: " + t2.spot.id);

		lot.showAvailability();

		exit1.exit("CAR-111");

		lot.showAvailability();
	}
}