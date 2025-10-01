import java.util.*;
import java.util.concurrent.locks.*;

// ---------------- Vehicle Types ----------------
abstract class Vehicle {
    private String licensePlate;
    public Vehicle(String licensePlate) {
        this.licensePlate = licensePlate;
    }
    public String getLicensePlate() {
        return licensePlate;
    }
    public abstract SpotType getRequiredSpotType();
}

class Car extends Vehicle {
    public Car(String licensePlate) { super(licensePlate); }
    @Override
    public SpotType getRequiredSpotType() { return SpotType.CAR; }
}

class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate) { super(licensePlate); }
    @Override
    public SpotType getRequiredSpotType() { return SpotType.MOTORCYCLE; }
}

class Truck extends Vehicle {
    public Truck(String licensePlate) { super(licensePlate); }
    @Override
    public SpotType getRequiredSpotType() { return SpotType.TRUCK; }
}

// ---------------- Spot Types ----------------
enum SpotType {
    MOTORCYCLE, CAR, TRUCK
}

// ---------------- Parking Spot ----------------
class ParkingSpot {
    private String id;
    private SpotType type;
    private boolean occupied;
    private Vehicle currentVehicle;

    public ParkingSpot(String id, SpotType type) {
        this.id = id;
        this.type = type;
        this.occupied = false;
    }

    public synchronized boolean assignVehicle(Vehicle v) {
        if (!occupied && v.getRequiredSpotType() == type) {
            occupied = true;
            currentVehicle = v;
            return true;
        }
        return false;
    }

    public synchronized void removeVehicle() {
        occupied = false;
        currentVehicle = null;
    }

    public boolean isAvailable() { return !occupied; }
    public SpotType getType() { return type; }
    public String getId() { return id; }
    public Vehicle getCurrentVehicle() { return currentVehicle; }
}

// ---------------- Level ----------------
class Level {
    private String levelId;
    private List<ParkingSpot> spots;

    public Level(String levelId, int carSpots, int bikeSpots, int truckSpots) {
        this.levelId = levelId;
        spots = new ArrayList<>();
        for (int i = 0; i < carSpots; i++) spots.add(new ParkingSpot(levelId + "-C" + i, SpotType.CAR));
        for (int i = 0; i < bikeSpots; i++) spots.add(new ParkingSpot(levelId + "-M" + i, SpotType.MOTORCYCLE));
        for (int i = 0; i < truckSpots; i++) spots.add(new ParkingSpot(levelId + "-T" + i, SpotType.TRUCK));
    }

    public synchronized ParkingSpot findAvailableSpot(Vehicle v) {
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable() && spot.getType() == v.getRequiredSpotType()) {
                if (spot.assignVehicle(v)) {
                    return spot;
                }
            }
        }
        return null;
    }

    public synchronized void releaseSpot(String spotId) {
        for (ParkingSpot spot : spots) {
            if (spot.getId().equals(spotId)) {
                spot.removeVehicle();
                return;
            }
        }
    }

    public synchronized int availableSpots(SpotType type) {
        int count = 0;
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable() && spot.getType() == type) count++;
        }
        return count;
    }
}

// ---------------- Parking Lot ----------------
class ParkingLot {
    private static ParkingLot instance;
    private List<Level> levels;
    private Lock lock = new ReentrantLock();

    private ParkingLot() {
        levels = new ArrayList<>();
    }

    public static synchronized ParkingLot getInstance() {
        if (instance == null) {
            instance = new ParkingLot();
        }
        return instance;
    }

    public void addLevel(Level level) {
        levels.add(level);
    }

    public ParkingSpot parkVehicle(Vehicle v) {
        lock.lock();
        try {
            for (Level level : levels) {
                ParkingSpot spot = level.findAvailableSpot(v);
                if (spot != null) {
                    System.out.println("Vehicle " + v.getLicensePlate() + " parked at spot " + spot.getId());
                    return spot;
                }
            }
            System.out.println("No available spot for vehicle " + v.getLicensePlate());
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void leaveSpot(ParkingSpot spot) {
        lock.lock();
        try {
            for (Level level : levels) {
                level.releaseSpot(spot.getId());
            }
            System.out.println("Spot " + spot.getId() + " is now free.");
        } finally {
            lock.unlock();
        }
    }

    public void displayAvailability() {
        System.out.println("---- Parking Availability ----");
        for (Level level : levels) {
            System.out.println("Level " + level + ": Cars=" + level.availableSpots(SpotType.CAR) +
                               ", Bikes=" + level.availableSpots(SpotType.MOTORCYCLE) +
                               ", Trucks=" + level.availableSpots(SpotType.TRUCK));
        }
    }
}

// ---------------- Entry and Exit Gates ----------------
class EntryGate {
    private String gateId;
    private ParkingLot lot;

    public EntryGate(String gateId, ParkingLot lot) {
        this.gateId = gateId;
        this.lot = lot;
    }

    public ParkingSpot park(Vehicle v) {
        System.out.println("EntryGate " + gateId + ": Vehicle " + v.getLicensePlate() + " entering.");
        return lot.parkVehicle(v);
    }
}

class ExitGate {
    private String gateId;
    private ParkingLot lot;

    public ExitGate(String gateId, ParkingLot lot) {
        this.gateId = gateId;
        this.lot = lot;
    }

    public void exit(ParkingSpot spot) {
        System.out.println("ExitGate " + gateId + ": Vehicle leaving from spot " + spot.getId());
        lot.leaveSpot(spot);
    }
}

// ---------------- Main Simulation ----------------
public class ParkingLotApp {
    public static void main(String[] args) {
        ParkingLot lot = ParkingLot.getInstance();

        // Add 2 levels
        lot.addLevel(new Level("L1", 2, 2, 1));
        lot.addLevel(new Level("L2", 1, 2, 1));

        EntryGate gate1 = new EntryGate("G1", lot);
        ExitGate exit1 = new ExitGate("X1", lot);

        // Vehicles
        Vehicle car1 = new Car("CAR-101");
        Vehicle bike1 = new Motorcycle("BIKE-202");
        Vehicle truck1 = new Truck("TRUCK-303");

        // Park Vehicles
        ParkingSpot spot1 = gate1.park(car1);
        ParkingSpot spot2 = gate1.park(bike1);
        ParkingSpot spot3 = gate1.park(truck1);

        lot.displayAvailability();

        // Exit Vehicle
        exit1.exit(spot2);

        lot.displayAvailability();
    }
}
