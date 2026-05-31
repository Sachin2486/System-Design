// Requirements : Design a Traffic Light System 🚦 that dynamically adjusts wait times based on real-time traffic data.



import java.util.*;

enum SignalState {
    RED,
    YELLOW,
    GREEN
}

/* ================= ROAD ================= */

class Road {
    String roadId;
    String name;
    int vehicleCount;
    
    Road(String roadId, String name, int vehicleCount) {
        
        this.roadId = roadId;
        this.name = name;
        this.vehicleCount = vehicleCount;
    }
    
    void updateTraffic(int vehicles) {
        this.vehicleCount = vehicles;
    }
}

/* ================= TRAFFIC LIGHT ================= */

class TrafficLight {
    
    SignalState currentState;
    
    TrafficLight() {
        currentState = SignalState.RED;
    }
    
    void setState(SignalState state) {
        currentState = state;
    }
    
    SignalState getState() {
        return currentState;
    }
}

/* ================= STRATEGY ================= */

interface TrafficStrategy {

    int calculateGreenTime(Road road);
}

/* ================= DEFAULT STRATEGY ================= */

class DynamicTrafficStrategy implements TrafficStrategy {

    @Override
    public int calculateGreenTime(Road road) {

        int vehicles = road.vehicleCount;

        if (vehicles >= 100)
            return 60;

        if (vehicles >= 50)
            return 40;

        return 20;
    }
}

/* ================= CONTROLLER ================= */

class TrafficController {

    private List<Road> roads;
    private TrafficStrategy strategy;

    TrafficController(List<Road> roads,
                      TrafficStrategy strategy) {

        this.roads = roads;
        this.strategy = strategy;
    }

    public void startCycle() {

        for (Road road : roads) {

            TrafficLight light = new TrafficLight();

            int greenTime =
                strategy.calculateGreenTime(road);

            light.setState(SignalState.GREEN);

            System.out.println(
                road.name +
                " -> GREEN for "
                + greenTime +
                " seconds"
            );

            light.setState(SignalState.YELLOW);

            System.out.println(
                road.name +
                " -> YELLOW for 5 seconds"
            );

            light.setState(SignalState.RED);

            System.out.println(
                road.name +
                " -> RED"
            );

            System.out.println();
        }
    }
}

/* ================= MAIN ================= */

public class Main {

    public static void main(String[] args) {

        Road north = new Road(
            "R1",
            "North Road",
            120
        );

        Road south = new Road(
            "R2",
            "South Road",
            30
        );

        Road east = new Road(
            "R3",
            "East Road",
            70
        );

        List<Road> roads =
            Arrays.asList(
                north,
                south,
                east
            );

        TrafficStrategy strategy =
            new DynamicTrafficStrategy();

        TrafficController controller =
            new TrafficController(
                roads,
                strategy
            );

        controller.startCycle();
    }
}
