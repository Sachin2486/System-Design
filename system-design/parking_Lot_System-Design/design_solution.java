import java.util.*;

enum VehicleType {
	MOTORCYCLE, CAR, TRUCK
}

// Vehicle class
class Vehicle {
	private String plateNumber;
	private VehicleType type;

	public Vehicle(String plateNumber, VehicleType type) {
		this.plateNumber = plateNumber;
		this.type = type;
	}

	public VehicleType getType() {
		return type;
	}

	public String getPlateNumber() {
		return plateNumber;
	}
}

// Parking spot that supports a specific vehicle type
class ParkingSpot {
	private int spotId;
	private VehicleType spotType;
	private boolean isOccupied;
	private Vehicle parkedVehicle;

	public ParkingSpot(int spotId, VehicleType spotType) {
		this.spotId = spotId;
		this.spotType = spotType;
		this.isOccupied = false;
		this.parkedVehicle = null;
	}

	public boolean canFitVehicle(Vehicle vehicle) {
		return !isOccupied && vehicle.getType() == spotType;
	}

	public synchronized boolean park(Vehicle vehicle) {
		if(canFitVehicle(vehicle)) {
			parkedVehicle = vehicle;
			isOccupied = true;
			return true;
		}
		return false;
	}

	public synchronized void leave() {
		parkedVehicle = null;
		isOccupied = false;
	}

	public boolean isOccupied() {
		return isOccupied;
	}

	public VehicleType getSpotType() {
		return spotType;
	}

	public Vehicle getParkedVehicle() {
		return parkedVehicle;
	}

	public int getSpotId() {
		return spotId;
	}
}

// Parking level that contains multiple spots
class ParkingLevel {
	private int levelId;
	private List<ParkingSpot> spots;

	public ParkingLevel(int levelId, int numMotorcycleSpots, int numCarSpots, int numTruckSpots) {
		this.levelId = levelId;
		spots = new ArrayList<>();

		int spotCounter = 1;
		for(int i=0; i<numMotorcycleSpots; i++) {
			spots.add(new ParkingSpot(spotCounter++, VehicleType.MOTORCYCLE));
		}
		for(int i=0; i<numCarSpots; i++) {
			spots.add(new ParkingSpot(spotCounter++, VehicleType.CAR));
		}
		for(int i=0; i<numTruckSpots; i++) {
			spots.add(new ParkingSpot(spotCounter++, VehicleType.TRUCK));
		}
	}
	// Try to park vehicle in this level
	public synchronized boolean parkVehicle(Vehicle vehicle) {
		for(ParkingSpot spot : spots) {
			if(spot.park(vehicle)) {
				System.out.println("Parked " + vehicle.getPlateNumber() + " at Level " + levelId + " Spot " + spot.getSpotId());
				return true;
			}
		}
		return false;
	}

	// Free a spot by vehicle plate
	public synchronized boolean leaveVehicle(String plateNumber) {
		for (ParkingSpot spot : spots) {
			if (spot.isOccupied() && spot.getParkedVehicle().getPlateNumber().equals(plateNumber)) {
				spot.leave();
				System.out.println("Vehicle " + plateNumber + " left from Level " + levelId + " Spot " + spot.getSpotId());
				return true;
			}
		}
		return false;
	}

	// Count free spots by type
	public int availableSpots(VehicleType type) {
		int count = 0;
		for(ParkingSpot spot : spots) {
			if(!spot.isOccupied() && spot.getSpotType() == type) {
				count++;
			}
		}
		return count;
	}
}

// Main parking lot that manages multiple levels
class ParkingLot {
	private List <ParkingLevel> levels;

	public ParkingLot(int numLevels, int motorPerLevel, int carPerLevel, int truckPerLevel) {
		levels = new ArrayList<>();
		for(int i=1; i <= numLevels; i++) {
			levels.add(new ParkingLevel(i, motorPerLevel, carPerLevel, truckPerLevel));
		}
	}

	// try to park vehicle in any available level
	public boolean parkVehicle(Vehicle vehicle) {
		for(ParkingLevel level : levels) {
			if(level.parkVehicle(vehicle)) {
				return true;
			}
		}
		System.out.println("No parking available for "+ vehicle.getPlateNumber());
		return false;
	}

	// Remove vehicle
	public boolean leaveVehicle(String plateNumber) {
		for(ParkingLevel level : levels) {
			if(level.leaveVehicle(plateNumber)) {
				return true;
			}
		}
		System.out.println("Vehicle " + plateNumber + " not found!");
		return false;
	}

	// Show real-time availability
	public void displayAvailability() {
		for (ParkingLevel level : levels) {
			System.out.println("Level " + level + " Availability: Cars="
			                   + level.availableSpots(VehicleType.CAR) +
			                   " Motorcycles=" + level.availableSpots(VehicleType.MOTORCYCLE) +
			                   " Trucks=" + level.availableSpots(VehicleType.TRUCK));
		}
	}

}

public class ParkingLotSystem {
	public static void main (String[] args) {
		// Create parking lot with 2 levels (each having 2 motorcycle, 2 car, 1 truck spots)
		ParkingLot parkingLot = new ParkingLot(2, 2, 2, 1);

		// Vehicles
		Vehicle v1 = new Vehicle("MH12AB1234", VehicleType.CAR);
		Vehicle v2 = new Vehicle("MH14XY9876", VehicleType.TRUCK);
		Vehicle v3 = new Vehicle("MH01ZZ1111", VehicleType.MOTORCYCLE);

		// Park vehicles
		parkingLot.parkVehicle(v1);
		parkingLot.parkVehicle(v2);
		parkingLot.parkVehicle(v3);

		// Show availability
		parkingLot.displayAvailability();

		// Vehicle leaves
		parkingLot.leaveVehicle("MH12AB1234");

		// Show availability again
		parkingLot.displayAvailability();
	}
}






