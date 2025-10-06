import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.text.DecimalFormat;

/*
Single-file Ride Sharing demo
- Run: javac RideSharingDemo.java && java RideSharingDemo
*/

// Simple latitude/longitude holder and distance calc (Haversine)
class Location {
    final double lat;
    final double lon;

    public Location(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    // Haversine distance in kilometers
    public static double distanceKm(Location a, Location b) {
        double R = 6371.0;
        double dLat = Math.toRadians(b.lat - a.lat);
        double dLon = Math.toRadians(b.lon - a.lon);
        double rlat1 = Math.toRadians(a.lat);
        double rlat2 = Math.toRadians(b.lat);

        double sinDLat = Math.sin(dLat/2);
        double sinDLon = Math.sin(dLon/2);
        double x = sinDLat*sinDLat + Math.cos(rlat1)*Math.cos(rlat2)*sinDLon*sinDLon;
        double c = 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1-x));
        return R * c;
    }

    @Override
    public String toString() {
        return String.format("(%.5f, %.5f)", lat, lon);
    }
}

// Ride types with multiplier for fare calculation
enum RideType {
    REGULAR(1.0), PREMIUM(1.6), POOL(0.8);

    public final double multiplier;
    RideType(double m) { multiplier = m; }
}

// Passenger: user who requests rides
// Helps: allow passengers to request rides and get notifications
class Passenger {
    final String id;
    final String name;
    final NotificationHandler notifier;

    public Passenger(String id, String name, NotificationHandler notifier) {
        this.id = id;
        this.name = name;
        this.notifier = notifier;
    }
}

// Driver states
enum DriverStatus { OFFLINE, AVAILABLE, ON_RIDE }

// Driver: person who accepts and fulfills ride requests
// Helps: drivers accept/decline and update their location/status
class Driver {
    final String id;
    final String name;
    private volatile Location location;
    private final Lock lock = new ReentrantLock();
    private volatile DriverStatus status = DriverStatus.AVAILABLE;
    private volatile String currentRideId = null;

    public Driver(String id, String name, Location startLoc) {
        this.id = id;
        this.name = name;
        this.location = startLoc;
    }

    public Location getLocation() { return location; }
    public void setLocation(Location loc) { lock.lock(); try { this.location = loc; } finally { lock.unlock(); } }
    public DriverStatus getStatus() { return status; }
    public void setStatus(DriverStatus s) { this.status = s; }
    public void assignRide(String rideId) { this.currentRideId = rideId; this.status = DriverStatus.ON_RIDE; }
    public void clearRide() { this.currentRideId = null; this.status = DriverStatus.AVAILABLE; }

    @Override
    public String toString() {
        return String.format("Driver[%s:%s] @%s status=%s", id, name, location, status);
    }
}

// Ride request created by a passenger
// Helps: capture pickup/destination and desired ride type
class RideRequest {
    final String requestId;
    final Passenger passenger;
    final Location pickup;
    final Location destination;
    final RideType type;
    final Instant requestTime;

    public RideRequest(Passenger p, Location pickup, Location dest, RideType type) {
        this.requestId = UUID.randomUUID().toString();
        this.passenger = p;
        this.pickup = pickup;
        this.destination = dest;
        this.type = type;
        this.requestTime = Instant.now();
    }
}

// Ride status
enum RideStatus { REQUESTED, MATCHED, ACCEPTED, ENROUTE_TO_PICKUP, IN_PROGRESS, COMPLETED, CANCELLED }

// Ride: represents an assigned, running or completed ride
// Helps: track driver/passenger, status, start/end times and fare
class Ride {
    final String rideId;
    final RideRequest request;
    volatile RideStatus status;
    volatile Driver driver;
    volatile Instant acceptTime;
    volatile Instant startTime;
    volatile Instant endTime;
    volatile double fare;
    volatile String paymentTransactionId;

    public Ride(RideRequest req) {
        this.rideId = UUID.randomUUID().toString();
        this.request = req;
        this.status = RideStatus.REQUESTED;
    }

    public void setDriver(Driver d) {
        this.driver = d;
        this.status = RideStatus.MATCHED;
        this.acceptTime = Instant.now();
    }
}

// Simple notification handler (Observer consumer)
interface NotificationHandler {
    void notify(String message);
}

// Simple payment processor (mock)
// Helps: process payments and return transaction id
class PaymentProcessor {
    private final Random rnd = new Random();
    public String chargePassenger(Passenger p, double amount) {
        // mock processing delay
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        return "txn-" + Math.abs(rnd.nextInt());
    }
    public boolean payDriver(Driver d, double amount) {
        // mock pay; always success in demo
        return true;
    }
}

// Matching strategy: nearest available driver within radius
// Helps: match requests to drivers based on proximity
class Matcher {
    private final double maxKm; // max distance to consider

    public Matcher(double maxKm) {
        this.maxKm = maxKm;
    }

    // returns best driver or null
    public Driver findBestDriver(RideRequest req, Collection<Driver> drivers) {
        double bestDist = Double.MAX_VALUE;
        Driver best = null;
        for (Driver d : drivers) {
            if (d.getStatus() != DriverStatus.AVAILABLE) continue;
            double dist = Location.distanceKm(d.getLocation(), req.pickup);
            if (dist <= maxKm && dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
    }
}

// FareCalculator: simple fare computation based on distance (km), time (minutes), and ride type
// Helps: compute fare for each ride
class FareCalculator {
    private final double basePerKm = 10.0;    // ₹ per km
    private final double basePerMin = 1.5;    // ₹ per minute
    private final double baseFee = 25.0;      // base booking fee
    private final DecimalFormat fmt = new DecimalFormat("#.##");

    public double estimateFare(Location a, Location b, RideType type) {
        double kms = Location.distanceKm(a, b);
        double mins = Math.max(1.0, kms / 40.0 * 60.0); // assume avg speed 40 km/h
        double raw = baseFee + kms * basePerKm + mins * basePerMin;
        return Double.parseDouble(fmt.format(raw * type.multiplier));
    }

    public double finalizeFare(Instant start, Instant end, Location a, Location b, RideType type) {
        double kms = Location.distanceKm(a, b);
        double mins = Math.max(1.0, Duration.between(start, end).toMinutes());
        double raw = baseFee + kms * basePerKm + mins * basePerMin;
        return Double.parseDouble(fmt.format(raw * type.multiplier));
    }
}

// Notification service to send updates to passengers and drivers
// Helps: deliver real-time ride status updates
class Notifier {
    public void notifyPassenger(Passenger p, String message) {
        p.notifier.notify(message);
    }
    public void notifyDriver(Driver d, String message) {
        // In real system driver has notification channel; for demo use console print
        System.out.println("[DriverNotify] " + d.name + ": " + message);
    }
}

// RideManager: facade that accepts requests, matches drivers, processes rides and payments
// Helps: core orchestration for requests, matching, ride lifecycle and payments
class RideManager {
    private final ConcurrentHashMap<String, Driver> drivers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Ride> activeRides = new ConcurrentHashMap<>();
    private final BlockingQueue<RideRequest> requestQueue = new LinkedBlockingQueue<>();
    private final Matcher matcher;
    private final FareCalculator fareCalculator = new FareCalculator();
    private final PaymentProcessor paymentProcessor = new PaymentProcessor();
    private final Notifier notifier = new Notifier();
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);
    private final ExecutorService matchPool = Executors.newFixedThreadPool(4);

    private volatile boolean running = true;

    public RideManager(Matcher matcher) {
        this.matcher = matcher;
        // start background matcher thread
        matchPool.submit(this::matchLoop);
    }

    public void registerDriver(Driver d) {
        drivers.put(d.id, d);
    }

    public void unregisterDriver(String driverId) {
        drivers.remove(driverId);
    }

    // Passenger requests a ride; returns created Ride object
    public Ride requestRide(RideRequest rr) {
        Ride ride = new Ride(rr);
        requestQueue.offer(rr);
        notifier.notifyPassenger(rr.passenger, "Your ride request " + rr.requestId + " placed. Finding driver...");
        return ride; // note: not yet in activeRides until matched/accepted
    }

    // Main matching loop - picks requests and tries to assign a driver
    private void matchLoop() {
        while (running) {
            try {
                RideRequest req = requestQueue.take();
                // attempt match
                Driver d = matcher.findBestDriver(req, drivers.values());
                if (d == null) {
                    notifier.notifyPassenger(req.passenger, "No drivers nearby. Please wait or try later.");
                    // we can requeue with delay; for demo just continue
                    continue;
                }

                // create Ride and store (tentative)
                Ride ride = new Ride(req);
                ride.setDriver(d); // matched
                ride.status = RideStatus.MATCHED;
                activeRides.put(ride.rideId, ride);

                // notify driver and passenger
                notifier.notifyDriver(d, "New ride match: pickup at " + req.pickup + " for passenger " + req.passenger.name);
                notifier.notifyPassenger(req.passenger, "Driver " + d.name + " matched and notified.");

                // simulate driver decision to accept or decline asynchronously
                executor.submit(() -> driverDecisionFlow(d, ride));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Simulate driver accepting/declining and ride lifecycle
    private void driverDecisionFlow(Driver d, Ride ride) {
        // small simulated decision delay
        try { Thread.sleep(500 + new Random().nextInt(800)); } catch (InterruptedException ignored) {}

        // simple acceptance logic: driver accepts if available
        if (d.getStatus() != DriverStatus.AVAILABLE) {
            notifier.notifyPassenger(ride.request.passenger, "Driver " + d.name + " not available. Searching again.");
            activeRides.remove(ride.rideId);
            requestQueue.offer(ride.request); // requeue
            return;
        }

        // Accept
        d.assignRide(ride.rideId);
        ride.status = RideStatus.ACCEPTED;
        notifier.notifyDriver(d, "You accepted ride " + ride.rideId);
        notifier.notifyPassenger(ride.request.passenger, "Driver " + d.name + " accepted your ride. Arriving soon.");

        // Simulate drive to pickup
        ride.status = RideStatus.ENROUTE_TO_PICKUP;
        notifier.notifyDriver(d, "Driving to pickup...");
        notifier.notifyPassenger(ride.request.passenger, "Driver enroute to pickup.");

        // simulate time to pickup based on distance
        double toPickupKm = Location.distanceKm(d.getLocation(), ride.request.pickup);
        long pickupMs = (long)(toPickupKm / 30.0 * 3600_000); // assume 30 km/h
        pickupMs = Math.max(500, Math.min(pickupMs, 5000));
        try { Thread.sleep(pickupMs); } catch (InterruptedException ignored) {}

        // start ride
        ride.startTime = Instant.now();
        ride.status = RideStatus.IN_PROGRESS;
        notifier.notifyPassenger(ride.request.passenger, "Driver arrived. Ride started.");
        notifier.notifyDriver(d, "Ride started.");

        // simulate ride duration based on distance
        double tripKm = Location.distanceKm(ride.request.pickup, ride.request.destination);
        long tripMs = (long)(tripKm / 40.0 * 3600_000); // assume 40 km/h
        tripMs = Math.max(1000, Math.min(tripMs, 8000));
        try { Thread.sleep(tripMs); } catch (InterruptedException ignored) {}

        // complete ride
        ride.endTime = Instant.now();
        ride.status = RideStatus.COMPLETED;
        // compute fare
        ride.fare = fareCalculator.finalizeFare(ride.startTime, ride.endTime, ride.request.pickup, ride.request.destination, ride.request.type);

        // process payment
        String txn = paymentProcessor.chargePassenger(ride.request.passenger, ride.fare);
        ride.paymentTransactionId = txn;
        // pay driver (mock)
        paymentProcessor.payDriver(d, ride.fare * 0.8); // 80% to driver
        d.clearRide();

        // notify both
        notifier.notifyPassenger(ride.request.passenger, "Ride completed. Fare: ₹" + ride.fare + ". Txn: " + txn);
        notifier.notifyDriver(d, "Ride completed. You earned: ₹" + String.format("%.2f", ride.fare * 0.8));

        // cleanup
        activeRides.remove(ride.rideId);
    }

    // For demo/testing: list available drivers
    public List<Driver> getAvailableDriversSnapshot() {
        List<Driver> res = new ArrayList<>();
        for (Driver d : drivers.values()) if (d.getStatus() == DriverStatus.AVAILABLE) res.add(d);
        return res;
    }

    // For demo/testing: list active rides
    public List<Ride> getActiveRidesSnapshot() {
        return new ArrayList<>(activeRides.values());
    }

    public void shutdown() {
        running = false;
        matchPool.shutdownNow();
        executor.shutdownNow();
    }
}

// Demo NotificationHandler for passenger that prints to console
class PassengerConsoleNotifier implements NotificationHandler {
    private final String passengerName;
    public PassengerConsoleNotifier(String name) { this.passengerName = name; }
    public void notify(String message) {
        System.out.println("[PassengerNotify] " + passengerName + ": " + message);
    }
}

// Demo
public class RideSharingDemo {
    public static void main(String[] args) throws Exception {
        Matcher matcher = new Matcher(10.0); // max 10 km match radius
        RideManager manager = new RideManager(matcher);

        // Create drivers and register
        Driver d1 = new Driver("D1", "Ravi", new Location(19.0760, 72.8777)); // Mumbai
        Driver d2 = new Driver("D2", "Sneha", new Location(19.0700, 72.8800));
        Driver d3 = new Driver("D3", "Amit", new Location(19.2000, 72.9700)); // a bit further

        manager.registerDriver(d1);
        manager.registerDriver(d2);
        manager.registerDriver(d3);

        // Create passengers
        Passenger p1 = new Passenger("P1", "Alice", new PassengerConsoleNotifier("Alice"));
        Passenger p2 = new Passenger("P2", "Bob", new PassengerConsoleNotifier("Bob"));

        // Passenger 1 requests a regular ride
        RideRequest rr1 = new RideRequest(p1, new Location(19.0740, 72.8780), new Location(19.0950, 72.8860), RideType.REGULAR);
        manager.requestRide(rr1);

        // Passenger 2 requests a premium ride shortly after
        Thread.sleep(200);
        RideRequest rr2 = new RideRequest(p2, new Location(19.0710, 72.8790), new Location(19.1500, 72.9000), RideType.PREMIUM);
        manager.requestRide(rr2);

        // Let system run a bit for the demo
        Thread.sleep(20000);

        // Show snapshots
        System.out.println("\nAvailable drivers snapshot:");
        for (Driver d : manager.getAvailableDriversSnapshot()) System.out.println(d);

        System.out.println("\nActive rides snapshot:");
        for (Ride r : manager.getActiveRidesSnapshot()) System.out.println("Ride " + r.rideId + " status=" + r.status);

        // Shutdown
        manager.shutdown();
        System.out.println("Demo finished.");
    }
}
