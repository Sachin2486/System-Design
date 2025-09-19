import java.util.*;

// Vehicle size types
enum VehicleSize {
    SMALL,
    MEDIUM,
    LARGE
}

// Represents a vehicle that will be parked
class Vehicle {
    private final String plateNumber;
    private final VehicleSize size;

    public Vehicle(String plateNumber, VehicleSize size) {
        this.plateNumber = plateNumber;
        this.size = size;
    }
    
    public String getPlateNumber() {
        return plateNumber;
    }
    
    public VehicleSize getSize() {
        return size;
    }
    
    @Override
    public String toString() {
        return plateNumber + " (" + size + ")";
    }
}

// Represents a single parking spot
class ParkingSpot {
    private final int spotId;
    private final VehicleSize spotSize;
    private Vehicle parkedVehicle;

    public ParkingSpot(int spotId, VehicleSize spotSize) {
        this.spotId = spotId;
        this.spotSize = spotSize;
    }
    
    public synchronized boolean isAvailable() {
        return parkedVehicle == null;
    }
    
    // Small can fit in bigger, but not vice versa
    public synchronized boolean canFit(Vehicle vehicle) {
        return isAvailable() && vehicle.getSize().ordinal() <= spotSize.ordinal();
    }
    
    public synchronized boolean park(Vehicle vehicle) {
        if (canFit(vehicle)) {
            parkedVehicle = vehicle;
            return true;
        }
        return false;
    }

    public synchronized Vehicle leave() {
        Vehicle v = parkedVehicle;
        parkedVehicle = null;
        return v;
    }
    
    public Vehicle getParkedVehicle() {
        return parkedVehicle;
    }
    
    public int getSpotId() {
        return spotId;
    }
    
    public VehicleSize getSpotSize() {
        return spotSize;
    }
}

// Represents a parking ticket issued on entry
class Ticket {
    private static int counter = 1;

    private final int ticketId;
    private final Vehicle vehicle;
    private final ParkingSpot spot;
    private boolean isPaid;

    public Ticket(Vehicle vehicle, ParkingSpot spot) {
        this.ticketId = counter++;
        this.vehicle = vehicle;
        this.spot = spot;
        this.isPaid = false;
    }
    
    public void markPaid() {
        this.isPaid = true;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSpot getSpot() {
        return spot;
    }

    @Override
    public String toString() {
        return "Ticket#" + ticketId + " for " + vehicle + " at Spot " + spot.getSpotId() +
                " [Paid=" + isPaid + "]";
    }
}

// Manages all parking spots and ticket assignment
class ParkingLot {
    private final List<ParkingSpot> spots = new ArrayList<>();
    private final Map<String, Ticket> activeTickets = new HashMap<>(); // plate -> ticket
    
    public ParkingLot(List<ParkingSpot> spots) {
        this.spots.addAll(spots);
    }
    
    // Assign nearest available spot and create ticket
    public synchronized Ticket parkVehicle(Vehicle vehicle) {
        for (ParkingSpot spot : spots) {
            if (spot.canFit(vehicle)) {
                if(spot.park(vehicle)) {
                    Ticket ticket = new Ticket(vehicle, spot);
                    activeTickets.put(vehicle.getPlateNumber(), ticket);
                    return ticket;
                }
            }
        }
        return null; // no spot found
    }
    
    // Exit the vehicle, mark ticket paid
    public synchronized boolean leaveVehicle(String plateNumber) {
        Ticket ticket = activeTickets.get(plateNumber);
        if (ticket == null) return false;

        ticket.getSpot().leave();
        ticket.markPaid();
        activeTickets.remove(plateNumber);
        return true;
    }
    
    public synchronized void printUsedSpots() {
        System.out.println("Used Spots:");
        for (ParkingSpot spot : spots) {
            if (!spot.isAvailable()) {
                System.out.println(" Spot " + spot.getSpotId() + ": " + spot.getParkedVehicle());
            }
        }
    }

    public synchronized void printAvailableSpots() {
        System.out.println("Available Spots:");
        for (ParkingSpot spot : spots) {
            if (spot.isAvailable()) {
                System.out.println(" Spot " + spot.getSpotId() + " (" + spot.getSpotSize() + ")");
            }
        }
    }
}

public class ParkingLotDemo {
    public static void main(String[] args) {
        // Create parking spots (mixed sizes)
        List<ParkingSpot> spots = new ArrayList<>();
        spots.add(new ParkingSpot(1, VehicleSize.SMALL));
        spots.add(new ParkingSpot(2, VehicleSize.MEDIUM));
        spots.add(new ParkingSpot(3, VehicleSize.LARGE));
        spots.add(new ParkingSpot(4, VehicleSize.LARGE));

        ParkingLot lot = new ParkingLot(spots);

        Vehicle car = new Vehicle("MH12AB1234", VehicleSize.MEDIUM);
        Vehicle bike = new Vehicle("MH14XY5555", VehicleSize.SMALL);
        Vehicle truck = new Vehicle("MH20TR9999", VehicleSize.LARGE);

        // Park vehicles
        Ticket t1 = lot.parkVehicle(car);
        Ticket t2 = lot.parkVehicle(bike);
        Ticket t3 = lot.parkVehicle(truck);

        System.out.println("Issued Tickets:");
        System.out.println(t1);
        System.out.println(t2);
        System.out.println(t3);

        lot.printUsedSpots();
        lot.printAvailableSpots();

        // Exit a vehicle
        System.out.println("\nCar exiting...");
        lot.leaveVehicle("MH12AB1234");

        lot.printUsedSpots();
        lot.printAvailableSpots();
    }
}




