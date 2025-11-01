import java.util.*;

// ---- Enum for Vehicle Type ----
enum VehicleType {
    CAR,
    MOTORCYCLE,
    TRUCK
}

// ---- Vehicle Base Class ----
abstract class Vehicle {
    private String licensePlate;
    private final VehicleType type;
    
    public Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }
    
    public VehicleType getType() {
        return type;
    }
    
    public String getLicensePlate() {
        return licensePlate;
    }
}

// ---- Concrete Vehicle Classes ----
class Car extends Vehicle {
    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }
}

class Motorcycle extends Vehicle {
    public Motorcycle(String licensePlate) {
        super(licensePlate, VehicleType.MOTORCYCLE);
    }
}

class Truck extends Vehicle {
    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }
}

// ---- Parking Spot ----
class ParkingSpot {
    private final String id;
    private final VehicleType spotType;
    private boolean isOccupied;
    private Vehicle parkedVehicle;
    
    public ParkingSpot(String id,VehicleType type) {
        this.id = id;
        this.spotType = type;
        this.isOccupied = false;
    }
    
    public boolean canFitVehicle(Vehicle vehicle) {
        return !isOccupied && vehicle.getType() == spotType;
    }
    
    public synchronized void parkVehicle(Vehicle vehicle) {
        if (!canFitVehicle(vehicle)) {
            throw new IllegalStateException("Cannot park vehicle in this spot!");
        }
        this.parkedVehicle = vehicle;
        this.isOccupied = true;
        System.out.println("Vehicle " + vehicle.getLicensePlate() + " parked at spot " + id);
    }

    public synchronized void removeVehicle() {
        if (isOccupied) {
            System.out.println("Vehicle " + parkedVehicle.getLicensePlate() + " leaving spot " + id);
            this.parkedVehicle = null;
            this.isOccupied = false;
        }
    }
    
    public boolean isAvailable() {
        return !isOccupied;
    }

    public VehicleType getSpotType() {
        return spotType;
    }
    
    public synchronized boolean hasVehicle(String licensePlate) {
        return !isAvailable() && parkedVehicle != null && parkedVehicle.getLicensePlate().equals(licensePlate);
    }
}

// ---- Level ----
class Level {
    private final int levelNumber;
    private final List<ParkingSpot> spots;
    
    public Level(int levelNumber, List<ParkingSpot> spots) {
        this.levelNumber = levelNumber;
        this.spots = spots;
    }
    
    public synchronized boolean parkVehicle(Vehicle vehicle) {
        for(ParkingSpot spot: spots) {
            if(spot.canFitVehicle(vehicle)) {
                spot.parkVehicle(vehicle);
                return true;
            }
        }
        return false;
    }
    
    public synchronized boolean removeVehicle(String licensePlate) {
        for (ParkingSpot spot : spots) {
            if (spot.hasVehicle(licensePlate)) {
                spot.removeVehicle();
                return true;
            }
        }
        return false;
    }
    
    public long availableSpots(VehicleType type) {
        return spots.stream().filter(s -> s.isAvailable() && s.getSpotType() == type).count();
    }

    public int getLevelNumber() {
        return levelNumber;
    }
}

// ---- Parking Lot ----
class ParkingLot {
    private static ParkingLot instance;
    private final List<Level> levels;

    private ParkingLot(List<Level> levels) {
        this.levels = levels;
    }

    // Singleton
    public static synchronized ParkingLot getInstance(List<Level> levels) {
        if (instance == null) {
            instance = new ParkingLot(levels);
        }
        return instance;
    }

    public synchronized boolean parkVehicle(Vehicle vehicle) {
        for (Level level : levels) {
            if (level.parkVehicle(vehicle)) {
                System.out.println("Vehicle parked at Level " + level.getLevelNumber());
                return true;
            }
        }
        System.out.println("No available spot for " + vehicle.getLicensePlate());
        return false;
    }

    public synchronized boolean removeVehicle(String licensePlate) {
        for (Level level : levels) {
            if (level.removeVehicle(licensePlate)) {
                System.out.println("Vehicle removed from Level " + level.getLevelNumber());
                return true;
            }
        }
        return false;
    }

    public void displayAvailability() {
        System.out.println("\n--- Parking Availability ---");
        for (Level level : levels) {
            System.out.println("Level " + level.getLevelNumber() + ":");
            for (VehicleType type : VehicleType.values()) {
                System.out.println("  " + type + " spots available: " + level.availableSpots(type));
            }
        }
    }
}

// ---- Entry & Exit Gates ----
class EntryGate {
    public void vehicleEntry(Vehicle vehicle, ParkingLot lot) {
        System.out.println("\n[ENTRY] Vehicle " + vehicle.getLicensePlate() + " entering...");
        lot.parkVehicle(vehicle);
    }
}

class ExitGate {
    public void vehicleExit(String licensePlate, ParkingLot lot) {
        System.out.println("\n[EXIT] Vehicle " + licensePlate + " exiting...");
        lot.removeVehicle(licensePlate);
    }
}

// ---- Main Driver Code ----
public class ParkingLotSystem {
    public static void main(String[] args) {
        // Create parking spots for Level 1
        List<ParkingSpot> level1Spots = Arrays.asList(
                new ParkingSpot("L1S1", VehicleType.CAR),
                new ParkingSpot("L1S2", VehicleType.MOTORCYCLE),
                new ParkingSpot("L1S3", VehicleType.TRUCK)
        );

        // Create parking spots for Level 2
        List<ParkingSpot> level2Spots = Arrays.asList(
                new ParkingSpot("L2S1", VehicleType.CAR),
                new ParkingSpot("L2S2", VehicleType.CAR),
                new ParkingSpot("L2S3", VehicleType.MOTORCYCLE)
        );

        Level level1 = new Level(1, level1Spots);
        Level level2 = new Level(2, level2Spots);

        ParkingLot parkingLot = ParkingLot.getInstance(Arrays.asList(level1, level2));

        EntryGate entryGate = new EntryGate();
        ExitGate exitGate = new ExitGate();

        // Simulate
        Vehicle car1 = new Car("MH12AB1234");
        Vehicle bike1 = new Motorcycle("MH13CD5678");
        Vehicle truck1 = new Truck("MH14EF9012");

        parkingLot.displayAvailability();

        entryGate.vehicleEntry(car1, parkingLot);
        entryGate.vehicleEntry(bike1, parkingLot);
        entryGate.vehicleEntry(truck1, parkingLot);

        parkingLot.displayAvailability();

        exitGate.vehicleExit("MH13CD5678", parkingLot);
        parkingLot.displayAvailability();
    }
}


    