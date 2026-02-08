import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


/* ---------------- Vehicle Type ---------------- */
enum VehicleType {
    MOTORCYCLE,
    CAR,
    TRUCK
}

/* ---------------- Vehicle ---------------- */
abstract class Vehicle {
    final String number;
    final VehicleType type;

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

/* ---------------- Parking Spot ---------------- */
class ParkingSpot {
    final String spotId;
    final VehicleType supportedType;
    private boolean occupied = false;

    ParkingSpot(String spotId, VehicleType supportedType) {
        this.spotId = spotId;
        this.supportedType = supportedType;
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

/* ---------------- Parking Level ---------------- */
class ParkingLevel {
    final int levelId;
    private final Map<VehicleType, Queue<ParkingSpot>> freeSpots = new HashMap<>();

    ParkingLevel(int levelId) {
        this.levelId = levelId;
        for (VehicleType type : VehicleType.values()) {
            freeSpots.put(type, new LinkedList<>());
        }
    }

    void addSpot(ParkingSpot spot) {
        freeSpots.get(spot.supportedType).offer(spot);
    }

    ParkingSpot getAvailableSpot(Vehicle vehicle) {
        Queue<ParkingSpot> queue = freeSpots.get(vehicle.type);
        if (queue.isEmpty()) return null;

        ParkingSpot spot = queue.poll();
        spot.park();
        return spot;
    }

    void releaseSpot(ParkingSpot spot) {
        spot.leave();
        freeSpots.get(spot.supportedType).offer(spot);
    }

    int getAvailableCount(VehicleType type) {
        return freeSpots.get(type).size();
    }
}

/* ---------------- Parking Lot ---------------- */
class ParkingLot {

    private final List<ParkingLevel> levels = new ArrayList<>();
    private final Map<String, ParkingSpot> activeTickets = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    void addLevel(ParkingLevel level) {
        levels.add(level);
    }

    ParkingSpot parkVehicle(Vehicle vehicle) {
        lock.lock();
        try {
            for (ParkingLevel level : levels) {
                ParkingSpot spot = level.getAvailableSpot(vehicle);
                if (spot != null) {
                    activeTickets.put(vehicle.number, spot);
                    return spot;
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
            ParkingSpot spot = activeTickets.remove(vehicleNumber);
            if (spot == null) return;

            for (ParkingLevel level : levels) {
                level.releaseSpot(spot);
            }
        } finally {
            lock.unlock();
        }
    }

    void printAvailability() {
        System.out.println("---- Parking Availability ----");
        for (ParkingLevel level : levels) {
            System.out.println("Level " + level.levelId);
            for (VehicleType type : VehicleType.values()) {
                System.out.println(
                        "  " + type + " spots: " +
                        level.getAvailableCount(type)
                );
            }
        }
    }
}

/* ---------------- SINGLE MAIN ---------------- */
public class Main {
    public static void main(String[] args) {

        ParkingLot parkingLot = new ParkingLot();

        // Create Level 1
        ParkingLevel level1 = new ParkingLevel(1);
        level1.addSpot(new ParkingSpot("L1-C1", VehicleType.CAR));
        level1.addSpot(new ParkingSpot("L1-C2", VehicleType.CAR));
        level1.addSpot(new ParkingSpot("L1-M1", VehicleType.MOTORCYCLE));
        level1.addSpot(new ParkingSpot("L1-T1", VehicleType.TRUCK));

        // Create Level 2
        ParkingLevel level2 = new ParkingLevel(2);
        level2.addSpot(new ParkingSpot("L2-C1", VehicleType.CAR));
        level2.addSpot(new ParkingSpot("L2-M1", VehicleType.MOTORCYCLE));

        parkingLot.addLevel(level1);
        parkingLot.addLevel(level2);

        Vehicle car1 = new Car("CAR-101");
        Vehicle bike1 = new Motorcycle("BIKE-201");
        Vehicle truck1 = new Truck("TRUCK-301");

        ParkingSpot s1 = parkingLot.parkVehicle(car1);
        ParkingSpot s2 = parkingLot.parkVehicle(bike1);
        ParkingSpot s3 = parkingLot.parkVehicle(truck1);

        System.out.println("Car parked at: " + (s1 != null ? s1.spotId : "No spot"));
        System.out.println("Bike parked at: " + (s2 != null ? s2.spotId : "No spot"));
        System.out.println("Truck parked at: " + (s3 != null ? s3.spotId : "No spot"));

        parkingLot.printAvailability();

        System.out.println("\nUnparking CAR-101...");
        parkingLot.unparkVehicle("CAR-101");

        parkingLot.printAvailability();
    }
}
