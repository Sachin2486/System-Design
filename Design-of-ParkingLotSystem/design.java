import java.util.*;

// Enum for Vehicle Type
enum VehicleType {
	CAR, MOTORCYCLE, TRUCK
}

// Base Vehicle class
class Vehicle {
	private String licensePlate;
	private VehicleType type;

	public Vehicle(String licensePlate, VehicleType type) {
		this.licensePlate = licensePlate;
		this.type = type;
	}

	public String getLicensePlate() {
		return licensePlate;
	}

	public VehicleType getType() {
		return type;
	}
}

// Parking Spot class
class ParkingSpot {
	private int spotId;
	private VehicleType spotType;
	private boolean occupied;
	private Vehicle parkedVehicle;

	public ParkingSpot(int spotId, VehicleType spotType) {
		this.spotId = spotId;
		this.spotType = spotType;
		this.occupied = false;
	}

	public boolean isAvailable() {
		return !occupied;
	}

	public VehicleType getSpotType() {
		return spotType;
	}

	public int getSpotId() {
		return spotId;
	}

	public boolean parkVehicle(Vehicle vehicle) {
		if (occupied || vehicle.getType() != this.spotType) {
			return false;
		}
		this.parkedVehicle = vehicle;
		this.occupied = true;
		return true;
	}

	public boolean removeVehicle() {
		if (!occupied) return false;
		this.parkedVehicle = null;
		this.occupied = false;
		return true;
	}

	public String toString() {
		return "Spot " + spotId + " (" + spotType + ") - " + (occupied ? "Occupied" : "Free");
	}
}

class ParkingLevel {
    private int levelId;
    private List<ParkingSpot> spots;

    public ParkingLevel(int levelId, int carSpots, int bikeSpots, int truckSpots) {
        this.levelId = levelId;
        this.spots = new ArrayList<>();

        int idCounter = 1;
        for (int i = 0; i < carSpots; i++) spots.add(new ParkingSpot(idCounter++, VehicleType.CAR));
        for (int i = 0; i < bikeSpots; i++) spots.add(new ParkingSpot(idCounter++, VehicleType.MOTORCYCLE));
        for (int i = 0; i < truckSpots; i++) spots.add(new ParkingSpot(idCounter++, VehicleType.TRUCK));
    }
    
    public boolean parkVehicle(Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable() && spot.getSpotType() == vehicle.getType()) {
                spot.parkVehicle(vehicle);
                System.out.println("Vehicle " + vehicle.getLicensePlate() + " parked at Level " + levelId + ", Spot " + spot.getSpotId());
                return true;
            }
        }
        return false;
    }

    public boolean removeVehicle(String licensePlate) {
        for (ParkingSpot spot : spots) {
            if (!spot.isAvailable() && spot.toString().contains(licensePlate)) {
                spot.removeVehicle();
                System.out.println("Vehicle " + licensePlate + " exited from Level " + levelId + ", Spot " + spot.getSpotId());
                return true;
            }
        }
        return false;
    }

    public void displayAvailability() {
        System.out.println("\nLevel " + levelId + " Status:");
        for (ParkingSpot spot : spots) {
            System.out.println(spot);
        }
    }

    public boolean hasAvailableSpot(VehicleType type) {
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable() && spot.getSpotType() == type)
                return true;
        }
        return false;
    }
}

class ParkingLot {
    private static ParkingLot instance;
    private List<ParkingLevel> levels;

    private ParkingLot() {
        this.levels = new ArrayList<>();
    }

    public static synchronized ParkingLot getInstance() {
        if (instance == null) {
            instance = new ParkingLot();
        }
        return instance;
    }

    public void addLevel(ParkingLevel level) {
        levels.add(level);
    }

    public void parkVehicle(Vehicle vehicle) {
        for (ParkingLevel level : levels) {
            if (level.parkVehicle(vehicle)) {
                return;
            }
        }
        System.out.println("No available spot for " + vehicle.getType() + " (" + vehicle.getLicensePlate() + ")");
    }

    public void removeVehicle(String licensePlate) {
        for (ParkingLevel level : levels) {
            if (level.removeVehicle(licensePlate)) {
                return;
            }
        }
        System.out.println("Vehicle not found in any level.");
    }

    public void displayAvailability() {
        for (ParkingLevel level : levels) {
            level.displayAvailability();
        }
    }
}

// Main class (Entry point)
public class ParkingSystem {
    public static void main(String[] args) {
        ParkingLot lot = ParkingLot.getInstance();

        // Create two levels
        lot.addLevel(new ParkingLevel(1, 2, 2, 1));
        lot.addLevel(new ParkingLevel(2, 1, 2, 1));

        Vehicle car1 = new Vehicle("MH12AB1234", VehicleType.CAR);
        Vehicle bike1 = new Vehicle("MH14XY5678", VehicleType.MOTORCYCLE);
        Vehicle truck1 = new Vehicle("MH09TR9988", VehicleType.TRUCK);

        lot.parkVehicle(car1);
        lot.parkVehicle(bike1);
        lot.parkVehicle(truck1);

        lot.displayAvailability();

        System.out.println("\nReleasing one vehicle...");
        lot.removeVehicle("MH14XY5678");

        lot.displayAvailability();
    }
}

