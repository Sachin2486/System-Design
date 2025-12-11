import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

enum RideStatus { REQUESTED, ACCEPTED, CANCELLED, COMPLETED }

enum RideType { ECONOMY, PREMIUM }

class Location {
    final double lat;
    final double lon;
    Location(double lat, double lon) { this.lat = lat; this.lon = lon; }

    // Haversine formula to compute distance in kilometers
    double distanceTo(Location other) {
        double R = 6371; // Earth radius km
        double dLat = Math.toRadians(other.lat - lat);
        double dLon = Math.toRadians(other.lon - lon);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(other.lat)) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    @Override public String toString() { return String.format("(%f, %f)", lat, lon); }
}

class Passenger {
    final long id;
    final String name;
    Passenger(long id, String name) { this.id = id; this.name = name; }
}

class Driver {
    final long id;
    final String name;
    // driver's current location (could be updated externally)
    volatile Location location;
    // whether driver is available for new requests
    final AtomicBoolean available = new AtomicBoolean(true);

    Driver(long id, String name, Location loc) { this.id = id; this.name = name; this.location = loc; }
}

class Ride {
    final long id;
    final long passengerId;
    volatile Long driverId; // nullable until accepted
    final Location pickup;
    final Location destination;
    final RideType rideType;
    final Instant requestTime;
    volatile Instant startTime;
    volatile Instant endTime;
    volatile RideStatus status;

    Ride(long id, long passengerId, Location pickup, Location destination, RideType type) {
        this.id = id;
        this.passengerId = passengerId;
        this.pickup = pickup;
        this.destination = destination;
        this.rideType = type;
        this.requestTime = Instant.now();
        this.status = RideStatus.REQUESTED;
    }

    // Locking object for state transitions
    public Object lock() { return this; }

    @Override public String toString() {
        return String.format("Ride{id=%d, passenger=%d, driver=%s, from=%s, to=%s, type=%s, status=%s}",
                id, passengerId, driverId==null?"null":driverId.toString(), pickup, destination, rideType, status);
    }
}

class Fare {
    final double amount;
    final double distanceKm;
    final long durationMinutes;
    Fare(double amount, double distanceKm, long durationMinutes) {
        this.amount = amount; this.distanceKm = distanceKm; this.durationMinutes = durationMinutes;
    }
    @Override public String toString() {
        return String.format("Fare(amount=%.2f, distanceKm=%.2f, durationMin=%d)", amount, distanceKm, durationMinutes);
    }
}

class FareCalculator {
    // Basic fare parameters (could be configurable)
    private static final double BASE_ECONOMY = 25.0; // base fare INR
    private static final double PER_KM_ECONOMY = 10.0; // per km
    private static final double PER_MIN_ECONOMY = 1.5; // per minute

    private static final double BASE_PREMIUM = 50.0;
    private static final double PER_KM_PREMIUM = 18.0;
    private static final double PER_MIN_PREMIUM = 3.0;

    static Fare calculate(Ride ride) {
        double distance = ride.pickup.distanceTo(ride.destination); // km
        long durationMin = 0;
        if (ride.startTime != null && ride.endTime != null) {
            durationMin = Math.max(1, Duration.between(ride.startTime, ride.endTime).toMinutes()); // at least 1 min
        }
        double amount;
        if (ride.rideType == RideType.ECONOMY) {
            amount = BASE_ECONOMY + PER_KM_ECONOMY * distance + PER_MIN_ECONOMY * durationMin;
        } else {
            amount = BASE_PREMIUM + PER_KM_PREMIUM * distance + PER_MIN_PREMIUM * durationMin;
        }
        // round to 2 decimals
        amount = Math.round(amount * 100.0) / 100.0;
        return new Fare(amount, distance, durationMin);
    }
}

class RideService {
    private final ConcurrentMap<Long, Passenger> passengers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Driver> drivers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Ride> rides = new ConcurrentHashMap<>();

    private final AtomicLong passengerIdGen = new AtomicLong(1000);
    private final AtomicLong driverIdGen = new AtomicLong(2000);
    private final AtomicLong rideIdGen = new AtomicLong(3000);

    // Register helper methods
    long registerPassenger(String name) {
        long id = passengerIdGen.getAndIncrement();
        passengers.put(id, new Passenger(id, name));
        return id;
    }
    long registerDriver(String name, Location loc) {
        long id = driverIdGen.getAndIncrement();
        drivers.put(id, new Driver(id, name, loc));
        return id;
    }
    void updateDriverLocation(long driverId, Location loc) {
        Driver d = drivers.get(driverId);
        if (d != null) d.location = loc;
    }
    // Request a ride (passenger) => ride created in REQUESTED state
    long requestRide(long passengerId, Location pickup, Location dest, RideType type) {
        if (!passengers.containsKey(passengerId)) throw new IllegalArgumentException("passenger not found");
        long id = rideIdGen.getAndIncrement();
        Ride ride = new Ride(id, passengerId, pickup, dest, type);
        rides.put(id, ride);
        return id;
    }

    // List nearby drivers (simple radius filter sorted by distance)
    List<Long> findNearbyDrivers(long rideId, double radiusKm) {
        Ride ride = rides.get(rideId);
        if (ride == null) return Collections.emptyList();
        ArrayList<Map.Entry<Long, Double>> list = new ArrayList<>();
        for (Driver d : drivers.values()) {
            if (d.available.get()) {
                double dist = d.location.distanceTo(ride.pickup);
                if (dist <= radiusKm) list.add(Map.entry(d.id, dist));
            }
        }
        list.sort(Comparator.comparingDouble(Map.Entry::getValue));
        ArrayList<Long> ids = new ArrayList<>();
        for (var e : list) ids.add(e.getKey());
        return ids;
    }

    // Driver accepts ride
    boolean acceptRide(long driverId, long rideId) {
        Ride ride = rides.get(rideId);
        Driver driver = drivers.get(driverId);
        if (ride == null || driver == null) return false;
        synchronized (ride.lock()) {
            if (ride.status != RideStatus.REQUESTED) return false; // only accept if requested
            // assign driver
            ride.driverId = driverId;
            ride.status = RideStatus.ACCEPTED;
            ride.startTime = Instant.now();
            // mark driver unavailable
            driver.available.set(false);
            return true;
        }
    }

    // Passenger cancels ride
    boolean cancelRide(long passengerId, long rideId) {
        Ride ride = rides.get(rideId);
        if (ride == null) return false;
        synchronized (ride.lock()) {
            if (ride.passengerId != passengerId) return false; // only owner can cancel
            // allow cancellation only if not completed or cancelled
            if (ride.status == RideStatus.COMPLETED || ride.status == RideStatus.CANCELLED) return false;
            ride.status = RideStatus.CANCELLED;
            // if driver was assigned, free them
            if (ride.driverId != null) {
                Driver d = drivers.get(ride.driverId);
                if (d != null) d.available.set(true);
            }
            return true;
        }
    }

    // Driver completes the ride
    Fare completeRide(long driverId, long rideId) {
        Ride ride = rides.get(rideId);
        Driver driver = drivers.get(driverId);
        if (ride == null || driver == null) throw new IllegalArgumentException("ride or driver invalid");
        synchronized (ride.lock()) {
            if (!Objects.equals(ride.driverId, driverId)) throw new IllegalStateException("driver not assigned to this ride");
            if (ride.status != RideStatus.ACCEPTED) throw new IllegalStateException("ride must be in ACCEPTED to complete");
            ride.endTime = Instant.now();
            ride.status = RideStatus.COMPLETED;
            // free driver
            driver.available.set(true);
            // calculate fare
            return FareCalculator.calculate(ride);
        }
    }

    // Query ride
    Ride getRide(long rideId) { return rides.get(rideId); }

    // For demo/testing: get simple lists
    List<Ride> listAllRides() { return new ArrayList<>(rides.values()); }
    List<Driver> listDrivers() { return new ArrayList<>(drivers.values()); }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        RideService service = new RideService();

        // Register passenger and two drivers
        long p1 = service.registerPassenger("Alice");
        long d1 = service.registerDriver("Bob", new Location(28.6139, 77.2090));    // Delhi approx
        long d2 = service.registerDriver("Charlie", new Location(28.7041, 77.1025)); // New Delhi nearby

        // Passenger requests a ride
        Location pickup = new Location(28.6139, 77.2090); // same as d1
        Location dest   = new Location(28.5355, 77.3910); // Noida approx
        long rideId = service.requestRide(p1, pickup, dest, RideType.ECONOMY);
        System.out.println("Ride requested: " + service.getRide(rideId));

        // Find nearby drivers within 10 km
        List<Long> nearby = service.findNearbyDrivers(rideId, 15.0);
        System.out.println("Nearby drivers: " + nearby);

        // Driver accepts
        if (!nearby.isEmpty()) {
            long acceptDriverId = nearby.get(0);
            boolean accepted = service.acceptRide(acceptDriverId, rideId);
            System.out.println("Driver " + acceptDriverId + " accepted? " + accepted);
            System.out.println("Ride after accept: " + service.getRide(rideId));
        }

        // simulate trip duration
        System.out.println("Simulating trip for 3 seconds (represents minutes in real system)...");
        Thread.sleep(3000); // for demo only

        // Complete ride
        Ride ride = service.getRide(rideId);
        if (ride.driverId != null) {
            Fare fare = service.completeRide(ride.driverId, rideId);
            System.out.println("Ride completed. Fare: " + fare);
            System.out.println("Final ride state: " + service.getRide(rideId));
        }

        // Demonstrate cancellation attempt after completion (should fail)
        boolean canceled = service.cancelRide(p1, rideId);
        System.out.println("Attempt to cancel after completion: " + canceled);

        // Show all rides
        System.out.println("All rides:");
        for (Ride r : service.listAllRides()) System.out.println(r);
    }
}
