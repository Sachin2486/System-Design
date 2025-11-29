import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

abstract class Vehicle {
	private final String number;
	private final VehicleType type;

	public Vehicle(String number, VehicleType type) {
		this.number = number;
		this.type = type;
	}

	public String getNumber() {
		return number;
	}

	public VehicleType getType() {
		return type;
	}
}

class Car extends Vehicle {
	public Car(String number) {
		super(number, VehicleType.CAR);
	}
}

class Motorcycle extends Vehicle {
	public Motorcycle(String number) {
		super(number, VehicleType.MOTORCYCLE);
	}
}

class Truck extends Vehicle {
	public Truck(String number) {
		super(number, VehicleType.TRUCK);
	}
}

enum VehicleType {
	MOTORCYCLE, CAR, TRUCK
}

enum SpotType {
	MOTORCYCLE, CAR, TRUCK;

	public boolean canFit(VehicleType type) {
		switch (this) {
		case MOTORCYCLE:
			return type == VehicleType.MOTORCYCLE;
		case CAR:
			return type == VehicleType.MOTORCYCLE || type == VehicleType.CAR;
		case TRUCK:
			return true; // Any vehicle can fit
		default:
			return false;
		}
	}
}

class ParkingSpot {
	private final int id;
	private final int level;
	private final SpotType spotType;
	private Vehicle vehicle;

	public ParkingSpot(int id, int level, SpotType type) {
		this.id = id;
		this.level = level;
		this.spotType = type;
	}

	public synchronized boolean isAvailable() {
		return vehicle == null;
	}

	public synchronized boolean canFitVehicle(Vehicle v) {
		return spotType.canFit(v.getType());
	}

	public synchronized boolean park(Vehicle v) {
		if(!isAvailable() || !canFitVehicle(v)) return false;
		this.vehicle = v;
		return true;
	}

	public synchronized void free() {
		this.vehicle = null;
	}

	public int getId() {
		return id;
	}
	
	public int getLevel() {
		return level;
	}
	
	public SpotType getSpotType() {
		return spotType;
	}
}

class Level {
    private final int levelId;
    private final List<ParkingSpot> spots;
    private final ReentrantLock lock = new ReentrantLock();

    public Level(int id, List<ParkingSpot> spots) {
        this.levelId = id;
        this.spots = spots;
    }

    public ParkingSpot parkVehicle(Vehicle v) {
        lock.lock();
        try {
            for (ParkingSpot spot : spots) {
                if (spot.isAvailable() && spot.canFitVehicle(v)) {
                    if (spot.park(v)) {
                        return spot;
                    }
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public long getAvailable(SpotType type) {
        return spots.stream()
                .filter(s -> s.getSpotType() == type && s.isAvailable())
                .count();
    }

    public int getLevelId() {
        return levelId;
    }
}

// ======================= PARKING LOT (Singleton) ========================

class ParkingLot {

    private static volatile ParkingLot instance;
    private final List<Level> levels = new ArrayList<>();
    private final ConcurrentHashMap<String, ParkingSpot> vehicleMap = new ConcurrentHashMap<>();

    private ParkingLot() {}

    public static ParkingLot getInstance() {
        if (instance == null) {
            synchronized (ParkingLot.class) {
                if (instance == null) instance = new ParkingLot();
            }
        }
        return instance;
    }

    public void addLevel(Level level) {
        levels.add(level);
    }

    public ParkingSpot park(Vehicle v) {
        for (Level level : levels) {
            ParkingSpot spot = level.parkVehicle(v);
            if (spot != null) {
                vehicleMap.put(v.getNumber(), spot);
                System.out.println("PARK SUCCESS → " + v.getNumber() +
                        " at Level " + spot.getLevel() +
                        ", Spot " + spot.getId());
                return spot;
            }
        }
        System.out.println("NO SPOT available for " + v.getNumber());
        return null;
    }

    public boolean exit(String vehicleNum) {
        ParkingSpot spot = vehicleMap.remove(vehicleNum);
        if (spot == null) {
            System.out.println("Vehicle " + vehicleNum + " not found!");
            return false;
        }
        spot.free();
        System.out.println("EXIT SUCCESS → " + vehicleNum + " freed Level "
                + spot.getLevel() + ", Spot " + spot.getId());
        return true;
    }

    public void printAvailability() {
        System.out.println("\n===== SPOT AVAILABILITY =====");
        for (SpotType type : SpotType.values()) {
            long total = 0;
            for (Level l : levels) {
                total += l.getAvailable(type);
            }
            System.out.println(type + " spots free = " + total);
        }
        System.out.println("=============================\n");
    }
}

// ======================= GATES ========================

class EntryGate {
    private final String name;

    public EntryGate(String name) {
        this.name = name;
    }

    public void enter(Vehicle v) {
        System.out.println("\n[" + name + "] Vehicle Entering: " + v.getNumber());
        ParkingLot.getInstance().park(v);
    }
}

class ExitGate {
    private final String name;

    public ExitGate(String name) {
        this.name = name;
    }

    public void exit(String vehicleNumber) {
        System.out.println("\n[" + name + "] Vehicle Exiting: " + vehicleNumber);
        ParkingLot.getInstance().exit(vehicleNumber);
    }
}

// ======================= DEMO ========================

public class ParkingLotDemo {
    public static void main(String[] args) {
    
    ParkingLot lot = ParkingLot.getInstance();

        // Create Levels
        lot.addLevel(createLevel(0));
        lot.addLevel(createLevel(1));

        lot.printAvailability();

        // Gates
        EntryGate g1 = new EntryGate("Gate-1");
        EntryGate g2 = new EntryGate("Gate-2");
        ExitGate x1 = new ExitGate("Exit-1");

        // Vehicles
        Vehicle c1 = new Car("MH12AB1234");
        Vehicle m1 = new Motorcycle("KA01XY9999");
        Vehicle t1 = new Truck("DL05TR9089");

        // Parking
        g1.enter(c1);
        g1.enter(m1);
        g2.enter(t1);

        lot.printAvailability();

        // Exit
        x1.exit(c1.getNumber());

        lot.printAvailability();
    }

    // Helper to create spots for one level
    private static Level createLevel(int levelId) {
        List<ParkingSpot> spots = new ArrayList<>();
        int idCount = levelId * 100;

        // 5 motorcycle spots
        for (int i = 0; i < 5; i++) spots.add(new ParkingSpot(idCount++, levelId, SpotType.MOTORCYCLE));

        // 10 car spots
        for (int i = 0; i < 10; i++) spots.add(new ParkingSpot(idCount++, levelId, SpotType.CAR));

        // 3 truck spots
        for (int i = 0; i < 3; i++) spots.add(new ParkingSpot(idCount++, levelId, SpotType.TRUCK));

        return new Level(levelId, spots);
    }
}
