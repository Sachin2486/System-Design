import java.util.*;
import java.time.LocalDateTime;

public class UberSystem {

    public static void main(String[] args) {
        // Mock data
        Rider rider = new Rider("rider1", new Location(12.9716, 77.5946)); // Bangalore coordinates
        Location riderLocation = rider.getLocation();

        Driver driver1 = new Driver("driver1", "Alice", new Location(12.9722, 77.5932), DriverStatus.AVAILABLE);
        Driver driver2 = new Driver("driver2", "Bob", new Location(12.9802, 77.5906), DriverStatus.AVAILABLE);

        DriverRepository driverRepository = new DriverRepository();
        driverRepository.addDriver(driver1);
        driverRepository.addDriver(driver2);

        RideRepository rideRepository = new RideRepository();

        RideService rideService = new RideService(driverRepository, rideRepository);
        Ride ride = rideService.requestRide(rider, riderLocation);

        if (ride != null) {
            System.out.println("Ride created with Driver: " + ride.getDriver().getName());
            ride.startRide();
            Location endLocation = new Location(12.9538, 77.4900); // Mock end location
            ride.completeRide(endLocation);
        } else {
            System.out.println("No drivers available.");
        }
    }
}

// --- Models ---
class Rider {
    private String id;
    private Location location;

    public Rider(String id, Location location) {
        this.id = id;
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public String getId() {
        return id;
    }
}

class Driver {
    private String id;
    private String name;
    private Location location;
    private DriverStatus status;
    private double averageRating;

    public Driver(String id, String name, Location location, DriverStatus status) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
}

class Ride {
    private String id;
    private Rider rider;
    private Driver driver;
    private RideStatus status;
    private Location startLocation;
    private Location endLocation;
    private LocalDateTime completedAt;

    public Ride(String id, Rider rider, Driver driver, RideStatus status) {
        this.id = id;
        this.rider = rider;
        this.driver = driver;
        this.status = status;
        this.startLocation = rider.getLocation();
    }

    public void startRide() {
        this.status = RideStatus.IN_PROGRESS;
        this.startLocation = rider.getLocation();
        System.out.println("Ride started by driver: " + driver.getName());
    }

    public void completeRide(Location endLocation) {
        this.status = RideStatus.COMPLETED;
        this.endLocation = endLocation;
        this.completedAt = LocalDateTime.now();
        System.out.println("Ride completed. Fare: $" + calculateFare());
    }

    private double calculateFare() {
        double distance = LocationService.getDistance(this.startLocation, this.endLocation);
        double baseFare = 5.00; // Base fare
        double ratePerKm = 1.50; // $1.50 per kilometer
        return baseFare + (distance * ratePerKm);
    }

    public Driver getDriver() {
        return driver;
    }

    public String getId() {
        return id;
    }
}

class Location {
    private double latitude;
    private double longitude;

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

// --- Enums ---
enum DriverStatus {
    AVAILABLE,
    EN_ROUTE,
    BUSY
}

enum RideStatus {
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED
}

// --- Services ---
class RideService {
    private DriverRepository driverRepository;
    private RideRepository rideRepository;

    public RideService(DriverRepository driverRepository, RideRepository rideRepository) {
        this.driverRepository = driverRepository;
        this.rideRepository = rideRepository;
    }

    public Ride requestRide(Rider rider, Location riderLocation) {
        // Step 1: Find nearby available drivers
        List<Driver> nearbyDrivers = driverRepository.findNearbyDrivers(riderLocation, 5); // Within 5 km

        // Step 2: Send ride request to each nearby driver
        for (Driver driver : nearbyDrivers) {
            if (sendRideRequest(driver, rider)) {
                // Step 3: Create ride once a driver accepts
                return createRide(rider, driver);
            }
        }

        // If no driver accepts, return null
        return null;
    }

    private boolean sendRideRequest(Driver driver, Rider rider) {
        // Mocking driver response for simplicity
        return Math.random() > 0.5; // Randomly accept the ride
    }

    private Ride createRide(Rider rider, Driver driver) {
        Ride ride = new Ride(UUID.randomUUID().toString(), rider, driver, RideStatus.ASSIGNED);
        rideRepository.save(ride);

        // Update driver status
        driver.setStatus(DriverStatus.EN_ROUTE);
        driverRepository.save(driver);

        return ride;
    }
}

class LocationService {
    public static double getDistance(Location start, Location end) {
        // Haversine formula to calculate distance between two locations
        double earthRadius = 6371; // in kilometers
        double latDiff = Math.toRadians(end.getLatitude() - start.getLatitude());
        double lonDiff = Math.toRadians(end.getLongitude() - start.getLongitude());

        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2)
                 + Math.cos(Math.toRadians(start.getLatitude())) * Math.cos(Math.toRadians(end.getLatitude()))
                 * Math.sin(lonDiff / 2) * Math.sin(lonDiff / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}

// --- Repositories ---
class DriverRepository {
    private List<Driver> drivers = new ArrayList<>();

    public List<Driver> findNearbyDrivers(Location location, int radius) {
        // Simulate searching for drivers within the given radius
        return drivers;
    }

    public void addDriver(Driver driver) {
        drivers.add(driver);
    }

    public void save(Driver driver) {
        // In a real-world scenario, update driver status in a database
    }
}

class RideRepository {
    private List<Ride> rides = new ArrayList<>();

    public void save(Ride ride) {
        rides.add(ride);
    }
}
