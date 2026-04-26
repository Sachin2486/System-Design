import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/* ================= ENUMS ================= */
enum RideStatus {
	REQUESTED, ACCEPTED, STARTED, COMPLETED
}

enum RideType {
	REGULAR, PREMIUM
}

class Location {
	double lat, lon;

	Location(double lat,double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	double distance(Location other) {
		return Math.sqrt(Math.pow(lat - other.lat, 2) +
		                 Math.pow(lon - other.lon, 2));
	}
}

class Passenger {
	String name;
	String id;

	Passenger(String id, String name) {
		this.id = id;
		this.name = name;
	}
}

class Driver {
	String id, name;
	Location location;
	boolean available = true;

	Driver(String id, String name, Location loc) {
		this.id = id;
		this.name = name;
		this.location = loc;
	}
}

interface FareStrategy {
	double calculate (double distance, double time);
}

class RegularFare implements FareStrategy {
	public double calculate(double distance, double time) {
		return 20 + distance * 10 + time * 2;
	}
}

class PremiumFare implements FareStrategy {
	public double calculate(double distance, double time) {
		return 50 + distance * 20 + time * 5;
	}
}

class Ride {
	String id;
	Passenger passenger;
	Driver driver;
	Location pickup;
	Location drop;
	RideType type;
	RideStatus status;
	double fare;

	ReentrantLock lock = new ReentrantLock();

	Ride(String id, Passenger p, Location pickup, Location drop, RideType type) {
		this.id = id;
		this.passenger = p;
		this.pickup = pickup;
		this.drop = drop;
		this.type = type;
		this.status = RideStatus.REQUESTED;
	}
}


class RideService {
	Map<String, Ride> rides = new HashMap<>();
	Map<String, Driver> drivers = new HashMap<>();

	/* Request Ride */
	Ride requestRide(Passenger p, Location pickup, Location drop, RideType type) {
		Ride ride = new Ride(UUID.randomUUID().toString(), p, pickup, drop, type);
		rides.put(ride.id, ride);
		return ride;
	}

	/* Match Driver */
	Driver findNearestDriver(Location pickup) {
		Driver best = null;
		double min = Double.MAX_VALUE;

		for (Driver d : drivers.values()) {
			if (d.available) {
				double dist = d.location.distance(pickup);
				if (dist < min) {
					min = dist;
					best = d;
				}
			}
		}
		return best;
	}

	/* Accept Ride (Concurrency Safe) */
	boolean acceptRide(String driverId, String rideId) {
		Ride ride = rides.get(rideId);
		Driver driver = drivers.get(driverId);

		ride.lock.lock();
		try {
			if (ride.status != RideStatus.REQUESTED) return false;

			ride.driver = driver;
			ride.status = RideStatus.ACCEPTED;
			driver.available = false;
			return true;
		} finally {
			ride.lock.unlock();
		}
	}

	/* Complete Ride */
	void completeRide(String rideId) {
		Ride ride = rides.get(rideId);

		FareStrategy strategy =
		    ride.type == RideType.REGULAR ?
		    new RegularFare() : new PremiumFare();

		double distance = ride.pickup.distance(ride.drop);
		ride.fare = strategy.calculate(distance, 10);

		ride.status = RideStatus.COMPLETED;
		ride.driver.available = true;

		System.out.println("Ride completed. Fare = " + ride.fare);
	}

}

public class Main
{
	public static void main(String[] args) {
		RideService service = new RideService();

		Driver d1 = new Driver("D1", "John", new Location(0,0));
		service.drivers.put("D1", d1);

		Passenger p = new Passenger("P1", "Sachin");

		Ride ride = service.requestRide(
		                p,
		                new Location(1,1),
		                new Location(5,5),
		                RideType.REGULAR
		            );

		Driver driver = service.findNearestDriver(ride.pickup);

		boolean accepted = service.acceptRide(driver.id, ride.id);

		System.out.println("Ride accepted: " + accepted);

		service.completeRide(ride.id);
	}
}
