import java.util.*;

// Represents an elevator request (user presses button)
class Request {
    int sourceFloor;
    int destinationFloor;

    public Request(int sourceFloor, int destinationFloor) {
        this.sourceFloor = sourceFloor;
        this.destinationFloor = destinationFloor;
    }

    public int getDirection() {
        return (destinationFloor > sourceFloor) ? 1 : -1;
    }
}

// Elevator states
enum ElevatorDirection { UP, DOWN, IDLE }

class Elevator {
    private int id;
    private int currentFloor;
    private int capacity;
    private int currentLoad;
    private ElevatorDirection direction;
    private Queue<Request> requests;

    public Elevator(int id, int capacity) {
        this.id = id;
        this.capacity = capacity;
        this.currentFloor = 0; // start at ground floor
        this.currentLoad = 0;
        this.direction = ElevatorDirection.IDLE;
        this.requests = new LinkedList<>();
    }

    public int getCurrentFloor() { return currentFloor; }
    public ElevatorDirection getDirection() { return direction; }
    public boolean isFull() { return currentLoad >= capacity; }
    public int getId() { return id; }

    // Assign a new request to this elevator
    public void addRequest(Request req) {
        requests.offer(req);
        if (direction == ElevatorDirection.IDLE) {
            direction = (req.getDirection() == 1) ? ElevatorDirection.UP : ElevatorDirection.DOWN;
        }
        System.out.println("Request assigned to Elevator " + id + " [From " + req.sourceFloor + " to " + req.destinationFloor + "]");
    }

    // Simulate elevator moving one step
    public void step() {
        if (requests.isEmpty()) {
            direction = ElevatorDirection.IDLE;
            return;
        }

        Request currentRequest = requests.peek();

        if (currentFloor < currentRequest.sourceFloor) {
            currentFloor++;
            direction = ElevatorDirection.UP;
        } else if (currentFloor > currentRequest.sourceFloor) {
            currentFloor--;
            direction = ElevatorDirection.DOWN;
        } else {
            // At source floor → pick passenger if not full
            if (!isFull()) {
                currentLoad++;
                System.out.println("Elevator " + id + " picked passenger at Floor " + currentFloor);
                // Move towards destination
                if (currentFloor < currentRequest.destinationFloor) {
                    currentFloor++;
                    direction = ElevatorDirection.UP;
                } else if (currentFloor > currentRequest.destinationFloor) {
                    currentFloor--;
                    direction = ElevatorDirection.DOWN;
                } else {
                    // Already at destination
                    requests.poll(); // remove request
                    currentLoad--;
                    System.out.println("Elevator " + id + " dropped passenger at Floor " + currentFloor);
                }
            } else {
                System.out.println("Elevator " + id + " is full at Floor " + currentFloor);
            }
        }
    }
}

// Manages multiple elevators and assigns requests optimally
class ElevatorSystem {
    private List<Elevator> elevators;
    private int totalFloors;

    public ElevatorSystem(int numElevators, int totalFloors, int elevatorCapacity) {
        this.totalFloors = totalFloors;
        elevators = new ArrayList<>();
        for (int i = 0; i < numElevators; i++) {
            elevators.add(new Elevator(i + 1, elevatorCapacity));
        }
    }

    // Assign request to the best elevator (nearest one going in right direction or idle)
    public void requestElevator(int source, int destination) {
        Request req = new Request(source, destination);
        Elevator bestElevator = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Elevator elevator : elevators) {
            if (!elevator.isFull()) {
                int distance = Math.abs(elevator.getCurrentFloor() - source);
                if (distance < bestDistance || elevator.getDirection() == ElevatorDirection.IDLE) {
                    bestElevator = elevator;
                    bestDistance = distance;
                }
            }
        }

        if (bestElevator != null) {
            bestElevator.addRequest(req);
        } else {
            System.out.println("All elevators are full! Please wait.");
        }
    }

    // Move all elevators one step
    public void step() {
        for (Elevator elevator : elevators) {
            elevator.step();
        }
    }
}

// Demo
public class ElevatorSimulation {
    public static void main(String[] args) {
        ElevatorSystem system = new ElevatorSystem(3, 10, 4); // 3 elevators, 10 floors, capacity 4

        // Users request elevators
        system.requestElevator(0, 5);
        system.requestElevator(3, 9);
        system.requestElevator(2, 0);

        // Simulate time steps
        for (int t = 0; t < 15; t++) {
            System.out.println("\n--- Time step " + t + " ---");
            system.step();
        }
    }
}
