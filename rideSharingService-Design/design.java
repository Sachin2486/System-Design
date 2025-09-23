import java.util.*;

// ---------------- Enums ----------------
enum RideType {
	REGULAR,
	PREMIUM
}

enum RideStatus {
	REQUESTED, ONGOING, COMPLETED, CANCELLED
}

// ---------------- Base User ----------------
abstract class User {
    protected String id;
    protected String name;
    
    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }
}

// ---------------- Passenger ----------------
class Passenger extends User {
    public Passenger(String id,String name) {
        super(id,name);
    }
    
    public Ride requestRide(String pickup, String destination, RideType type) {
        return new Ride(UUID.randomUUID().toString(), this, pickup, destination, type);
    }
}

class Driver extends User {
    private boolean available;
    private String location;
    
    public Driver(String id,String name, String location) {
        super(id,name);
        this.location = location;
        this.available = true;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void acceptRide(Ride ride) {
        if (!available) {
            System.out.println("Driver " + name + " is not available.");
            return;
        }
        ride.assignDriver(this);
        setAvailable(false);
        System.out.println("Driver " + name + " accepted the ride.");
    }

    public void completeRide(Ride ride) {
        ride.completeRide();
        setAvailable(true);
    }
}

// ---------------- Ride ----------------
class Ride{
    private String id;
    private Passenger passenger;
    private Driver driver;
    private String pickup;
    private String destination;
    private RideType type;
    private double fare;
    private RideStatus status;

    public Ride(String id, Passenger passenger, String pickup, String destination, RideType type) {
        this.id = id;
        this.passenger = passenger;
        this.pickup = pickup;
        this.destination = destination;
        this.type = type;
        this.status = RideStatus.REQUESTED;
    }
    
    public void assignDriver(Driver driver) {
        this.driver = driver;
        this.status = RideStatus.ONGOING;
        this.fare = FareCalculator.calculateFare(this);
        NotificationService.notify(passenger.name, "Driver" + driver.name + "assigned. Fare:" + fare);
    }
    
    public void completeRide() {
        this.status = RideStatus.COMPLETED;
        PaymentService.processPayment(passenger,driver,fare);
        NotificationService.notify(passenger.name, "Ride completed. Paid: " + fare);
        NotificationService.notify(driver.name, "Payment received: " + fare);
    }
    
    public RideType getType() {
        return type;
    }

    public RideStatus getStatus() {
        return status;
    }

    public String getPickup() {
        return pickup;
    }
}

// ---------------- Fare Calculator ----------------
class FareCalculator {
    public static double calculateFare(Ride ride) {
        double baseFare = ride.getType() == RideType.REGULAR ? 5.0 : 10.0;
        double distance = new Random().nextInt(10) + 1; // simulate distance
        double perKm = ride.getType() == RideType.REGULAR ? 2.0 : 5.0;
        return baseFare + (distance * perKm);
    }
}

// ---------------- Payment Service ----------------
class PaymentService {
    public static void processPayment(Passenger passenger, Driver driver, double amount) {
        System.out.println("Processing payment of " + amount + " from " + passenger.name + " to " + driver.name);
    }
}

// ---------------- Notification Service ----------------
class NotificationService {
    public static void notify(String userName, String message) {
        System.out.println("Notification to " + userName + ": " + message);
    }
}

// ---------------- Ride Service ----------------
class RideService {
    private List<Driver> drivers = new ArrayList<>();
    
    public void registerDriver(Driver driver) {
        drivers.add(driver);
    }
    
    public Driver findDriver(Ride ride) {
        for (Driver d : drivers) {
            if (d.isAvailable()) {
                return d;
            }
        }
        return null;
    }
    
    public void bookRide(Ride ride) {
        Driver driver = findDriver(ride);
        if(driver != null) {
            driver.acceptRide(ride);
        } else {
            System.out.println("No available drivers at the moment");
        }
    }
}

 // ---------------- Main ----------------
 public class RideSharingApp {
    public static void main (String[] args) {
         RideService rideService = new RideService();
         
         //Register Drivers
         Driver d1 = new Driver("D1", "Alice", "Loc1");
         Driver d2 = new Driver("D2", "Bob", "Loc2");
         rideService.registerDriver(d1);
         rideService.registerDriver(d2);
         
          // Passenger requests ride
        Passenger p1 = new Passenger("P1", "Sachin");
        Ride ride = p1.requestRide("Mumbai", "Pune", RideType.PREMIUM);
        
        // Book ride
        rideService.bookRide(ride);
    }
 }
 
