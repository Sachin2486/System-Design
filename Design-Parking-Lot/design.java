import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParkingLotSystem {

    // =====================================================
    // ENUMS
    // =====================================================

    enum VehicleType {
        MOTORCYCLE,
        CAR,
        TRUCK
    }

    enum SpotType {
        MOTORCYCLE,
        CAR,
        TRUCK
    }

    // =====================================================
    // VEHICLES
    // =====================================================

    static abstract class Vehicle {

        private final String vehicleNumber;
        private final VehicleType vehicleType;

        public Vehicle(String vehicleNumber,
                       VehicleType vehicleType) {

            this.vehicleNumber = vehicleNumber;
            this.vehicleType = vehicleType;
        }

        public String getVehicleNumber() {
            return vehicleNumber;
        }

        public VehicleType getVehicleType() {
            return vehicleType;
        }
    }

    static class Car extends Vehicle {

        public Car(String vehicleNumber) {
            super(vehicleNumber, VehicleType.CAR);
        }
    }

    static class Motorcycle extends Vehicle {

        public Motorcycle(String vehicleNumber) {
            super(vehicleNumber,
                    VehicleType.MOTORCYCLE);
        }
    }

    static class Truck extends Vehicle {

        public Truck(String vehicleNumber) {
            super(vehicleNumber,
                    VehicleType.TRUCK);
        }
    }

    // =====================================================
    // PARKING SPOT
    // =====================================================

    static class ParkingSpot {

        private final String spotId;
        private final SpotType spotType;

        private Vehicle parkedVehicle;

        public ParkingSpot(String spotId,
                           SpotType spotType) {

            this.spotId = spotId;
            this.spotType = spotType;
        }

        public String getSpotId() {
            return spotId;
        }

        public SpotType getSpotType() {
            return spotType;
        }

        public boolean isAvailable() {
            return parkedVehicle == null;
        }

        public void parkVehicle(
                Vehicle vehicle) {

            parkedVehicle = vehicle;
        }

        public void removeVehicle() {
            parkedVehicle = null;
        }
    }

    // =====================================================
    // LEVEL
    // =====================================================

    static class Level {

        private final int levelNumber;

        private final List<ParkingSpot> spots =
                new ArrayList<>();

        public Level(int levelNumber) {
            this.levelNumber = levelNumber;
        }

        public void addSpot(
                ParkingSpot spot) {

            spots.add(spot);
        }

        public List<ParkingSpot> getSpots() {
            return spots;
        }

        public int getLevelNumber() {
            return levelNumber;
        }
    }

    // =====================================================
    // TICKET
    // =====================================================

    static class ParkingTicket {

        private final String ticketId;

        private final Vehicle vehicle;

        private final ParkingSpot spot;

        private final long entryTime;

        public ParkingTicket(
                String ticketId,
                Vehicle vehicle,
                ParkingSpot spot) {

            this.ticketId = ticketId;
            this.vehicle = vehicle;
            this.spot = spot;
            this.entryTime =
                    System.currentTimeMillis();
        }

        public String getTicketId() {
            return ticketId;
        }

        public ParkingSpot getSpot() {
            return spot;
        }

        public Vehicle getVehicle() {
            return vehicle;
        }
    }

    // =====================================================
    // PARKING STRATEGY
    // =====================================================

    interface ParkingStrategy {

        ParkingSpot findSpot(
                Vehicle vehicle,
                List<Level> levels);
    }

    static class NearestSpotStrategy
            implements ParkingStrategy {

        @Override
        public ParkingSpot findSpot(
                Vehicle vehicle,
                List<Level> levels) {

            for (Level level : levels) {

                for (ParkingSpot spot :
                        level.getSpots()) {

                    if (!spot.isAvailable()) {
                        continue;
                    }

                    if (canFit(
                            vehicle.getVehicleType(),
                            spot.getSpotType())) {

                        return spot;
                    }
                }
            }

            return null;
        }

        private boolean canFit(
                VehicleType vehicleType,
                SpotType spotType) {

            switch (vehicleType) {

                case MOTORCYCLE:
                    return true;

                case CAR:
                    return spotType ==
                            SpotType.CAR ||
                            spotType ==
                                    SpotType.TRUCK;

                case TRUCK:
                    return spotType ==
                            SpotType.TRUCK;

                default:
                    return false;
            }
        }
    }

    // =====================================================
    // PARKING LOT
    // =====================================================

    static class ParkingLot {

        private final List<Level> levels =
                new ArrayList<>();

        private final ParkingStrategy strategy;

        private final Map<String,
                ParkingTicket> activeTickets =
                new ConcurrentHashMap<>();

        public ParkingLot(
                ParkingStrategy strategy) {

            this.strategy = strategy;
        }

        public void addLevel(Level level) {
            levels.add(level);
        }

        public synchronized ParkingTicket
        parkVehicle(Vehicle vehicle) {

            ParkingSpot spot =
                    strategy.findSpot(
                            vehicle,
                            levels);

            if (spot == null) {

                throw new RuntimeException(
                        "Parking Full");
            }

            spot.parkVehicle(vehicle);

            ParkingTicket ticket =
                    new ParkingTicket(
                            UUID.randomUUID()
                                    .toString(),
                            vehicle,
                            spot
                    );

            activeTickets.put(
                    ticket.getTicketId(),
                    ticket
            );

            return ticket;
        }

        public synchronized void
        unparkVehicle(String ticketId) {

            ParkingTicket ticket =
                    activeTickets.remove(
                            ticketId);

            if (ticket == null) {

                throw new RuntimeException(
                        "Invalid Ticket");
            }

            ticket.getSpot()
                    .removeVehicle();
        }

        public int getAvailableSpots() {

            int count = 0;

            for (Level level : levels) {

                for (ParkingSpot spot :
                        level.getSpots()) {

                    if (spot.isAvailable()) {
                        count++;
                    }
                }
            }

            return count;
        }
    }

    // =====================================================
    // ENTRY GATE
    // =====================================================

    static class EntryGate {

        private final String gateId;

        private final ParkingLot parkingLot;

        public EntryGate(
                String gateId,
                ParkingLot parkingLot) {

            this.gateId = gateId;
            this.parkingLot = parkingLot;
        }

        public ParkingTicket enter(
                Vehicle vehicle) {

            System.out.println(
                    "Entry Gate "
                            + gateId
                            + " processing vehicle "
                            + vehicle.getVehicleNumber());

            return parkingLot
                    .parkVehicle(vehicle);
        }
    }

    // =====================================================
    // EXIT GATE
    // =====================================================

    static class ExitGate {

        private final String gateId;

        private final ParkingLot parkingLot;

        public ExitGate(
                String gateId,
                ParkingLot parkingLot) {

            this.gateId = gateId;
            this.parkingLot = parkingLot;
        }

        public void exit(
                String ticketId) {

            System.out.println(
                    "Exit Gate "
                            + gateId);

            parkingLot
                    .unparkVehicle(ticketId);
        }
    }

    // =====================================================
    // DRIVER
    // =====================================================

    public static void main(String[] args) {

        ParkingLot parkingLot =
                new ParkingLot(
                        new NearestSpotStrategy()
                );

        Level level1 = new Level(1);

        level1.addSpot(
                new ParkingSpot(
                        "M1",
                        SpotType.MOTORCYCLE));

        level1.addSpot(
                new ParkingSpot(
                        "C1",
                        SpotType.CAR));

        level1.addSpot(
                new ParkingSpot(
                        "T1",
                        SpotType.TRUCK));

        parkingLot.addLevel(level1);

        EntryGate gate1 =
                new EntryGate(
                        "ENTRY-1",
                        parkingLot);

        ExitGate exit1 =
                new ExitGate(
                        "EXIT-1",
                        parkingLot);

        Vehicle car =
                new Car("KA01AB1234");

        ParkingTicket ticket =
                gate1.enter(car);

        System.out.println(
                "Allocated Spot : "
                        + ticket.getSpot()
                        .getSpotId());

        System.out.println(
                "Available Spots : "
                        + parkingLot
                        .getAvailableSpots());

        exit1.exit(
                ticket.getTicketId());

        System.out.println(
                "Available Spots : "
                        + parkingLot
                        .getAvailableSpots());
    }
}