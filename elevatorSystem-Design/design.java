import java.util.*;
import java.util.concurrent.*;

// Enum for Elevator Direction
enum Direction {
    UP, DOWN, IDLE
}

// Elevator class
class Elevator implements Runnable {
    private int elevatorId;
    private int currentFloor;
    private int capacity;
    private Direction direction;
    private Queue<Integer> targetFloors;
    private int passengers;
    private boolean running = true;

    public Elevator(int elevatorId, int capacity) {
        this.elevatorId = elevatorId;
        this.capacity = capacity;
        this.currentFloor = 0;
        this.direction = Direction.IDLE;
        this.targetFloors = new LinkedList<>();
        this.passengers = 0;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isAvailable() {
        return passengers < capacity;
    }

    public void addRequest(int floor) {
        synchronized (targetFloors) {
            targetFloors.offer(floor);
            targetFloors.notify();
        }
    }

    @Override
    public void run() {
        try {
            while (running) {
                int nextFloor;
                synchronized (targetFloors) {
                    while (targetFloors.isEmpty()) {
                        direction = Direction.IDLE;
                        targetFloors.wait();
                    }
                    nextFloor = targetFloors.poll();
                }
                moveTo(nextFloor);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void moveTo(int floor) throws InterruptedException {
        if (floor == currentFloor) return;

        direction = (floor > currentFloor) ? Direction.UP : Direction.DOWN;
        System.out.println("Elevator " + elevatorId + " moving " + direction + " from floor " + currentFloor + " to " + floor);

        while (currentFloor != floor) {
            if (direction == Direction.UP) currentFloor++;
            else currentFloor--;
            Thread.sleep(300); // simulate movement
        }

        System.out.println("Elevator " + elevatorId + " arrived at floor " + floor);
        direction = Direction.IDLE;
    }

    public void stopElevator() {
        running = false;
    }
}

// Request class
class ElevatorRequest {
    private int sourceFloor;
    private int destinationFloor;

    public ElevatorRequest(int source, int destination) {
        this.sourceFloor = source;
        this.destinationFloor = destination;
    }

    public int getSourceFloor() {
        return sourceFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public Direction getDirection() {
        return destinationFloor > sourceFloor ? Direction.UP : Direction.DOWN;
    }
}

// ElevatorController (Scheduler)
class ElevatorController {
    private static ElevatorController instance;
    private List<Elevator> elevators;
    private ExecutorService executorService;

    private ElevatorController(int elevatorCount, int capacity) {
        elevators = new ArrayList<>();
        executorService = Executors.newFixedThreadPool(elevatorCount);

        for (int i = 1; i <= elevatorCount; i++) {
            Elevator e = new Elevator(i, capacity);
            elevators.add(e);
            executorService.execute(e);
        }
    }

    public static synchronized ElevatorController getInstance(int elevatorCount, int capacity) {
        if (instance == null) {
            instance = new ElevatorController(elevatorCount, capacity);
        }
        return instance;
    }

    public void handleRequest(ElevatorRequest request) {
        Elevator bestElevator = findBestElevator(request);
        if (bestElevator != null) {
            System.out.println("Assigned Elevator " + bestElevator.hashCode() + " to handle request from floor " +
                    request.getSourceFloor() + " to " + request.getDestinationFloor());
            bestElevator.addRequest(request.getSourceFloor());
            bestElevator.addRequest(request.getDestinationFloor());
        } else {
            System.out.println("❌ No available elevator for request " + request.getSourceFloor() + " → " + request.getDestinationFloor());
        }
    }

    private Elevator findBestElevator(ElevatorRequest request) {
        Elevator best = null;
        int minDistance = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            int distance = Math.abs(e.getCurrentFloor() - request.getSourceFloor());
            if (e.isAvailable() && distance < minDistance) {
                minDistance = distance;
                best = e;
            }
        }
        return best;
    }

    public void shutdown() {
        for (Elevator e : elevators) {
            e.stopElevator();
        }
        executorService.shutdown();
    }
}

// Main Class (Entry point)
public class ElevatorSystem {
    public static void main(String[] args) throws InterruptedException {
        ElevatorController controller = ElevatorController.getInstance(3, 5);

        controller.handleRequest(new ElevatorRequest(0, 5));
        controller.handleRequest(new ElevatorRequest(3, 1));
        controller.handleRequest(new ElevatorRequest(6, 2));
        controller.handleRequest(new ElevatorRequest(2, 8));

        Thread.sleep(8000);
        controller.shutdown();
    }
}
