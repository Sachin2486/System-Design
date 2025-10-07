import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// ---- Domain ----
enum VehicleType { MOTORCYCLE, CAR, TRUCK }

class Vehicle {
    private final String plate;
    private final VehicleType type;
    public Vehicle(String plate, VehicleType type) { this.plate = plate; this.type = type; }
    public String getPlate() { return plate; }
    public VehicleType getType() { return type; }
    public String toString() { return plate + "(" + type + ")"; }
}

class ParkingSpot {
    private final String id;         // unique spot id (level-spot#)
    private final VehicleType type;  // spot supports this vehicle type
    private volatile boolean occupied = false;
    public ParkingSpot(String id, VehicleType type) { this.id = id; this.type = type; }
    public VehicleType getType() { return type; }
    public String getId() { return id; }

    // mark occupied - return true if successfully occupied
    public synchronized boolean occupy() {
        if (occupied) return false;
        occupied = true;
        return true;
    }

    public synchronized void free() {
        occupied = false;
    }

    public synchronized boolean isOccupied() { return occupied; }

    public String toString() { return "Spot[" + id + ":" + type + (occupied ? ":OCC" : ":FREE") + "]"; }
}

class Ticket {
    private final String ticketId;
    private final String plate;
    private final String spotId;
    private final Instant entryTime;
    public Ticket(String ticketId, String plate, String spotId) {
        this.ticketId = ticketId;
        this.plate = plate;
        this.spotId = spotId;
        this.entryTime = Instant.now();
    }
    public String getTicketId() { return ticketId; }
    public String getPlate() { return plate; }
    public String getSpotId() { return spotId; }
    public Instant getEntryTime() { return entryTime; }
    public String toString() {
        return "Ticket[" + ticketId + ", plate=" + plate + ", spot=" + spotId + ", entry=" + entryTime + "]";
    }
}

// ---- Parking Level ----
// Maintains per-type thread-safe queues of free spots for fast allocation.
class ParkingLevel {
    private final int levelId;
    private final Map<VehicleType, ConcurrentLinkedQueue<ParkingSpot>> freeByType = new EnumMap<>(VehicleType.class);
    private final Map<String, ParkingSpot> allSpots = new ConcurrentHashMap<>(); // spotId -> spot
    private final ReadWriteLock availabilityLock = new ReentrantReadWriteLock(); // used for consistent availability reads

    public ParkingLevel(int levelId, int numMotorcycleSpots, int numCarSpots, int numTruckSpots) {
        this.levelId = levelId;
        for (VehicleType t : VehicleType.values()) freeByType.put(t, new ConcurrentLinkedQueue<>());
        int counter = 1;
        for (int i = 0; i < numMotorcycleSpots; i++) addSpot(new ParkingSpot(levelId + "-M" + (counter++), VehicleType.MOTORCYCLE));
        for (int i = 0; i < numCarSpots; i++) addSpot(new ParkingSpot(levelId + "-C" + (counter++), VehicleType.CAR));
        for (int i = 0; i < numTruckSpots; i++) addSpot(new ParkingSpot(levelId + "-T" + (counter++), VehicleType.TRUCK));
    }

    private void addSpot(ParkingSpot s) {
        allSpots.put(s.getId(), s);
        freeByType.get(s.getType()).add(s);
    }

    public int getLevelId() { return levelId; }

    // Try to allocate a spot for vehicle type on this level.
    // Returns allocated ParkingSpot or null if none free.
    public ParkingSpot allocateSpot(VehicleType vehicleType) {
        // For this LLD, we only allow match of vehicle to spot type exactly.
        // (If you want small vehicles to fit larger spots, change the search order here.)
        availabilityLock.writeLock().lock();
        try {
            ConcurrentLinkedQueue<ParkingSpot> queue = freeByType.get(vehicleType);
            if (queue == null) return null;
            ParkingSpot spot;
            while ((spot = queue.poll()) != null) {
                // attempt to mark it occupied (spot occupancy is synchronized)
                if (spot.occupy()) {
                    return spot;
                } else {
                    // spot unexpectedly occupied: continue to next
                    continue;
                }
            }
            return null;
        } finally {
            availabilityLock.writeLock().unlock();
        }
    }

    // Free a spot by id and return true if successfully freed.
    public boolean freeSpot(String spotId) {
        ParkingSpot s = allSpots.get(spotId);
        if (s == null) return false;
        availabilityLock.writeLock().lock();
        try {
            synchronized (s) {
                if (!s.isOccupied()) return false; // already free
                s.free();
                freeByType.get(s.getType()).add(s);
                return true;
            }
        } finally {
            availabilityLock.writeLock().unlock();
        }
    }

    // Count available spots for a type
    public int availableSpots(VehicleType type) {
        availabilityLock.readLock().lock();
        try {
            ConcurrentLinkedQueue<ParkingSpot> q = freeByType.get(type);
            return q == null ? 0 : q.size();
        } finally {
            availabilityLock.readLock().unlock();
        }
    }

    // For real-time display, return a snapshot map type->count
    public Map<VehicleType, Integer> availabilitySnapshot() {
        Map<VehicleType, Integer> map = new EnumMap<>(VehicleType.class);
        availabilityLock.readLock().lock();
        try {
            for (VehicleType t : VehicleType.values()) map.put(t, availableSpots(t));
        } finally {
            availabilityLock.readLock().unlock();
        }
        return map;
    }

    public String toString() {
        return "Level-" + levelId;
    }
}

// ---- Parking Lot (manages levels, tickets, entry/exit gates) ----
class ParkingLot {
    private final List<ParkingLevel> levels = new ArrayList<>();
    private final ConcurrentMap<String, Ticket> ticketsById = new ConcurrentHashMap<>();   // ticketId -> Ticket
    private final ConcurrentMap<String, String> plateToTicket = new ConcurrentHashMap<>(); // plate -> ticketId
    // For concurrent access we attempt allocation per-level; per-level code is thread-safe
    public ParkingLot(int numLevels, int motorPerLevel, int carPerLevel, int truckPerLevel) {
        for (int i = 1; i <= numLevels; i++) levels.add(new ParkingLevel(i, motorPerLevel, carPerLevel, truckPerLevel));
    }

    // Entry gate: returns Ticket if parked, otherwise empty
    public Optional<Ticket> enterAndPark(Vehicle v) {
        // avoid duplicate parking for same plate
        if (plateToTicket.containsKey(v.getPlate())) {
            System.out.println("Vehicle already parked: " + v.getPlate());
            return Optional.empty();
        }

        // Try each level in order; we could use strategies (first-fit, nearest-level, least-filled)
        for (ParkingLevel level : levels) {
            ParkingSpot spot = level.allocateSpot(v.getType());
            if (spot != null) {
                String ticketId = UUID.randomUUID().toString();
                Ticket t = new Ticket(ticketId, v.getPlate(), spot.getId());
                ticketsById.put(ticketId, t);
                plateToTicket.put(v.getPlate(), ticketId);
                System.out.println("Parked " + v + " at " + level + " spot " + spot.getId() + " -> ticket=" + ticketId);
                return Optional.of(t);
            }
        }
        System.out.println("No spot available for " + v);
        return Optional.empty();
    }

    // Exit gate: free using ticket id
    public boolean exitAndFreeByTicket(String ticketId) {
        Ticket t = ticketsById.remove(ticketId);
        if (t == null) {
            System.out.println("Invalid ticket: " + ticketId);
            return false;
        }
        plateToTicket.remove(t.getPlate());
        // spot id encodes level as prefix (level-...), so find matching level and free
        String spotId = t.getSpotId();
        boolean freed = false;
        for (ParkingLevel level : levels) {
            // simple check: level id as prefix
            if (spotId.startsWith(level.getLevelId() + "-")) {
                freed = level.freeSpot(spotId);
                break;
            }
        }
        if (freed) {
            System.out.println("Vehicle " + t.getPlate() + " exited. Freed spot " + spotId + " (ticket " + ticketId + ")");
        } else {
            System.out.println("Failed to free spot " + spotId + " for ticket " + ticketId);
        }
        return freed;
    }

    // Exit by plate (if user kept plate)
    public boolean exitAndFreeByPlate(String plate) {
        String ticketId = plateToTicket.get(plate);
        if (ticketId == null) {
            System.out.println("No parked vehicle with plate: " + plate);
            return false;
        }
        return exitAndFreeByTicket(ticketId);
    }

    // Real-time availability across all levels
    public void displayAvailability() {
        System.out.println("---- Real-time Availability ----");
        for (ParkingLevel level : levels) {
            Map<VehicleType, Integer> snap = level.availabilitySnapshot();
            System.out.println(level + " => Cars=" + snap.get(VehicleType.CAR)
                    + ", Motorcycles=" + snap.get(VehicleType.MOTORCYCLE)
                    + ", Trucks=" + snap.get(VehicleType.TRUCK));
        }
        System.out.println("--------------------------------");
    }

    // helper to get number of free spots overall
    public Map<VehicleType, Integer> totalAvailability() {
        Map<VehicleType, Integer> total = new EnumMap<>(VehicleType.class);
        for (VehicleType t : VehicleType.values()) total.put(t, 0);
        for (ParkingLevel level : levels) {
            Map<VehicleType, Integer> snap = level.availabilitySnapshot();
            for (VehicleType t : VehicleType.values()) total.put(t, total.get(t) + snap.get(t));
        }
        return total;
    }
}

// ---- Entry and Exit Gate classes (simulate multiple entry/exit points) ----
class EntryGate implements Runnable {
    private final String gateName;
    private final ParkingLot lot;
    private final BlockingQueue<Vehicle> incoming = new LinkedBlockingQueue<>();
    public EntryGate(String gateName, ParkingLot lot) { this.gateName = gateName; this.lot = lot; }
    public void driveIn(Vehicle v) { incoming.add(v); }
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Vehicle v = incoming.poll(500, TimeUnit.MILLISECONDS);
                if (v == null) continue;
                System.out.println("[" + gateName + "] Processing entry for " + v);
                Optional<Ticket> t = lot.enterAndPark(v);
                if (t.isPresent()) {
                    System.out.println("[" + gateName + "] Issued " + t.get());
                } else {
                    System.out.println("[" + gateName + "] Could not park " + v);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

class ExitGate implements Runnable {
    private final String gateName;
    private final ParkingLot lot;
    private final BlockingQueue<String> outgoingTickets = new LinkedBlockingQueue<>();
    public ExitGate(String gateName, ParkingLot lot) { this.gateName = gateName; this.lot = lot; }
    public void driveOutWithTicket(String ticketId) { outgoingTickets.add(ticketId); }
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String ticketId = outgoingTickets.poll(500, TimeUnit.MILLISECONDS);
                if (ticketId == null) continue;
                System.out.println("[" + gateName + "] Processing exit for ticket " + ticketId);
                boolean ok = lot.exitAndFreeByTicket(ticketId);
                if (ok) System.out.println("[" + gateName + "] Exit successful for ticket " + ticketId);
                else System.out.println("[" + gateName + "] Exit failed for ticket " + ticketId);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}

// ---- Demo main ----
public class ParkingLotSystem {
    public static void main(String[] args) throws Exception {
        // Create a parking lot with 3 levels:
        // each level: 3 motorcycle spots, 5 car spots, 2 truck spots
        ParkingLot lot = new ParkingLot(3, 3, 5, 2);

        // Create two entry gates and two exit gates to simulate concurrency
        EntryGate entryA = new EntryGate("Entry-A", lot);
        EntryGate entryB = new EntryGate("Entry-B", lot);
        ExitGate exitA = new ExitGate("Exit-A", lot);
        ExitGate exitB = new ExitGate("Exit-B", lot);

        ExecutorService gatesPool = Executors.newFixedThreadPool(4);
        gatesPool.submit(entryA);
        gatesPool.submit(entryB);
        gatesPool.submit(exitA);
        gatesPool.submit(exitB);

        // Simulate incoming vehicles concurrently
        List<Vehicle> incomingVehicles = Arrays.asList(
                new Vehicle("MH01AA1111", VehicleType.CAR),
                new Vehicle("MH01AA1112", VehicleType.MOTORCYCLE),
                new Vehicle("MH01AA1113", VehicleType.CAR),
                new Vehicle("MH01AA1114", VehicleType.TRUCK),
                new Vehicle("MH01AA1115", VehicleType.CAR),
                new Vehicle("MH01AA1116", VehicleType.MOTORCYCLE),
                new Vehicle("MH01AA1117", VehicleType.TRUCK),
                new Vehicle("MH01AA1118", VehicleType.CAR),
                new Vehicle("MH01AA1119", VehicleType.CAR),
                new Vehicle("MH01AA1120", VehicleType.MOTORCYCLE)
        );

        // Feed vehicles to entry gates round-robin
        for (int i = 0; i < incomingVehicles.size(); i++) {
            if (i % 2 == 0) entryA.driveIn(incomingVehicles.get(i));
            else entryB.driveIn(incomingVehicles.get(i));
            Thread.sleep(80); // small gap to better simulate concurrency
        }

        // Let gates process
        Thread.sleep(1000);
        lot.displayAvailability();

        // For demo: collect some tickets by reading plate->ticket via trying to exit by plate
        // (In a real system you'd return ticket when parked; here exit by plate convenience)
        // We'll attempt to exit a few plates; to do that, map plates to tickets via internal state isn't public.
        // Instead we'll use exitAndFreeByPlate to simulate user showing plate at exit.
        System.out.println("\n-- Some vehicles leaving by plate --");
        lot.exitAndFreeByPlate("MH01AA1113"); // car
        lot.exitAndFreeByPlate("MH01AA1116"); // motorcycle

        // Another round of arrivals to verify freed spots reused
        System.out.println("\n-- More arrivals --");
        entryA.driveIn(new Vehicle("MH01AA1121", VehicleType.CAR));
        entryB.driveIn(new Vehicle("MH01AA1122", VehicleType.MOTORCYCLE));
        Thread.sleep(600);

        // Show final availability
        lot.displayAvailability();

        // Shutdown simulation
        gatesPool.shutdownNow();
        gatesPool.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println("Demo finished.");
    }
}
