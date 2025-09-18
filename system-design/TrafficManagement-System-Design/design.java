import java.time.LocalDateTime;
import java.util.*;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// Represents the color/state of a traffic light
enum SignalColor { RED, YELLOW, GREEN }

// TrafficLight: represents a single road's traffic light and its configurable durations.
// Helps match requirement: supports RED/YELLOW/GREEN with configurable durations per light.
class TrafficLight {
    private final String roadName;
    private volatile SignalColor color;
    private final long greenDurationMs;
    private final long yellowDurationMs;
    private final long redDurationMs;
    private final Lock lock = new ReentrantLock();

    public TrafficLight(String roadName, long greenMs, long yellowMs, long redMs) {
        this.roadName = roadName;
        this.greenDurationMs = greenMs;
        this.yellowDurationMs = yellowMs;
        this.redDurationMs = redMs;
        this.color = SignalColor.RED;
    }

    public String getRoadName() { return roadName; }

    // Thread-safe set color
    public void setColor(SignalColor newColor) {
        lock.lock();
        try {
            this.color = newColor;
            System.out.printf("[%s] %s -> %s at %s%n",
                    roadName, "LightChange", newColor, LocalDateTime.now());
        } finally {
            lock.unlock();
        }
    }

    public SignalColor getColor() {
        return this.color;
    }

    public long getGreenDurationMs() { return greenDurationMs; }
    public long getYellowDurationMs() { return yellowDurationMs; }
    public long getRedDurationMs() { return redDurationMs; }

    @Override
    public String toString() {
        return String.format("TrafficLight(%s: %s)", roadName, color);
    }
}

// EmergencyEvent: simple holder for emergency requests.
// Helps match requirement: allow detection and handling of emergency situations.
class EmergencyEvent {
    private final String roadName;
    private final Instant timestamp;

    public EmergencyEvent(String roadName) {
        this.roadName = roadName;
        this.timestamp = Instant.now();
    }

    public String getRoadName() { return roadName; }
    public Instant getTimestamp() { return timestamp; }
}

// IntersectionController: coordinates multiple traffic lights, cycles them, and handles emergencies.
// Helps match requirement: control flow at an intersection, configurable durations, smooth transitions, emergency handling.
class IntersectionController {
    private final Map<String, TrafficLight> lights = new LinkedHashMap<>(); // preserve ordering for cycle
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService notifier = Executors.newCachedThreadPool();
    private final Lock controllerLock = new ReentrantLock();

    // Use a queue for emergency events. Last emergency takes precedence.
    private final BlockingDeque<EmergencyEvent> emergencyQueue = new LinkedBlockingDeque<>();

    private volatile boolean running = false;

    // Create controller with lights; maintains cycle order as insertion order
    public IntersectionController(List<TrafficLight> trafficLights) {
        for (TrafficLight tl : trafficLights) {
            lights.put(tl.getRoadName(), tl);
        }
    }

    // Start normal cyclic operation
    public void start() {
        controllerLock.lock();
        try {
            if (running) return;
            running = true;
            // Start initial cycle scheduling on a separate thread
            scheduler.submit(this::cycleLoop);
            System.out.println("IntersectionController started.");
        } finally {
            controllerLock.unlock();
        }
    }

    // Stop the controller gracefully
    public void stop() {
        controllerLock.lock();
        try {
            running = false;
            scheduler.shutdownNow();
            notifier.shutdownNow();
            System.out.println("IntersectionController stopped.");
        } finally {
            controllerLock.unlock();
        }
    }

    // Report an emergency for a road; controller will preempt cycle to serve it
    public void reportEmergency(String roadName) {
        if (!lights.containsKey(roadName)) {
            System.out.println("Unknown road for emergency: " + roadName);
            return;
        }
        EmergencyEvent ev = new EmergencyEvent(roadName);
        // push so latest emergency is served first
        emergencyQueue.offerFirst(ev);
        System.out.println("Emergency reported for road: " + roadName + " at " + ev.getTimestamp());
    }

    // Main loop that cycles lights and checks for emergency events
    private void cycleLoop() {
        List<TrafficLight> cycleOrder = new ArrayList<>(lights.values());
        int idx = 0;
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // If there's an emergency, handle it first
                EmergencyEvent ev = emergencyQueue.poll();
                if (ev != null) {
                    handleEmergency(ev);
                    continue; // after emergency handling, continue loop to check for more
                }

                // Normal cycle for the current light
                TrafficLight current = cycleOrder.get(idx);
                // Set others to RED
                for (TrafficLight tl : cycleOrder) {
                    if (!tl.getRoadName().equals(current.getRoadName())) {
                        tl.setColor(SignalColor.RED);
                    }
                }

                // GREEN
                current.setColor(SignalColor.GREEN);
                sleepMillis(current.getGreenDurationMs());

                // Before switching, set YELLOW for smooth transition
                current.setColor(SignalColor.YELLOW);
                sleepMillis(current.getYellowDurationMs());

                // After yellow, set RED (next loop will set next green)
                current.setColor(SignalColor.RED);
                // optional short buffer (red) to avoid collisions
                sleepMillis(50);

                // move to next road
                idx = (idx + 1) % cycleOrder.size();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                System.err.println("Error in cycleLoop: " + ex.getMessage());
            }
        }
    }

    // Emergency handling: preempt normal cycle and give green to emergency road safely
    private void handleEmergency(EmergencyEvent ev) throws InterruptedException {
        String road = ev.getRoadName();
        System.out.println("Handling emergency for road: " + road);

        controllerLock.lock();
        try {
            // Strategy:
            // 1. Immediately set all non-emergency lights to RED (if currently GREEN, transition to YELLOW then RED).
            // 2. After they are safely RED, set emergency road to GREEN for a configurable emergency duration.
            // 3. After emergency clears, set emergency road to YELLOW then RED and resume normal cycles.

            // Transition any current GREEN to YELLOW then RED
            for (TrafficLight tl : lights.values()) {
                if (!tl.getRoadName().equals(road) && tl.getColor() == SignalColor.GREEN) {
                    tl.setColor(SignalColor.YELLOW);
                    // short yellow for preemption
                    sleepMillis(Math.min(tl.getYellowDurationMs(), 400));
                    tl.setColor(SignalColor.RED);
                }
            }

            // Ensure all others are RED
            for (TrafficLight tl : lights.values()) {
                if (!tl.getRoadName().equals(road)) {
                    tl.setColor(SignalColor.RED);
                }
            }

            // Now set emergency road GREEN
            TrafficLight emergencyLight = lights.get(road);
            // safety: give a small yellow buffer if emergency light is currently GREEN? we'll set directly
            emergencyLight.setColor(SignalColor.GREEN);

            // Hold green for emergency window (configurable). We'll pick 8 seconds here but can be parameterized.
            long emergencyHoldMs = 8000L;
            long elapsed = 0L;
            long step = 500L; // check queue every 500ms to allow overlapping emergency preemption
            while (elapsed < emergencyHoldMs) {
                // if there's a newer emergency for a different road, break to handle it first
                EmergencyEvent next = emergencyQueue.peekFirst();
                if (next != null && !next.getRoadName().equals(road)) {
                    System.out.println("Preempting emergency " + road + " for newer emergency " + next.getRoadName());
                    break;
                }
                sleepMillis(step);
                elapsed += step;
            }

            // End emergency: transition emergency green -> yellow -> red
            if (emergencyLight.getColor() == SignalColor.GREEN) {
                emergencyLight.setColor(SignalColor.YELLOW);
                sleepMillis(Math.min(emergencyLight.getYellowDurationMs(), 400));
                emergencyLight.setColor(SignalColor.RED);
            }
            System.out.println("Emergency handling complete for road: " + road);
        } finally {
            controllerLock.unlock();
        }
    }

    // Utility sleep wrapper to support interrupts
    private void sleepMillis(long ms) throws InterruptedException {
        if (ms <= 0) return;
        Thread.sleep(ms);
    }

    // For demo/debug: print current state of all lights
    public void printStatus() {
        System.out.println("---- Intersection Status ----");
        for (TrafficLight tl : lights.values()) {
            System.out.printf("Road %s : %s%n", tl.getRoadName(), tl.getColor());
        }
        System.out.println("-----------------------------");
    }
}

// Demo program to show the traffic signal system working
public class TrafficSignalSystemDemo {
    public static void main(String[] args) throws Exception {
        // Create traffic lights for multiple roads with configurable durations (ms)
        TrafficLight north = new TrafficLight("North", 5000, 1000, 6000);
        TrafficLight east  = new TrafficLight("East", 4000, 1000, 5000);
        TrafficLight south = new TrafficLight("South", 5000, 1000, 6000);
        TrafficLight west  = new TrafficLight("West", 4000, 1000, 5000);

        // Create intersection controller with lights in a specific cycle order
        IntersectionController controller = new IntersectionController(Arrays.asList(north, east, south, west));

        // Start the controller (begins cycling)
        controller.start();

        // Let it run for a while
        Thread.sleep(7000);
        controller.printStatus();

        // Simulate an emergency approaching from "East"
        System.out.println("\n>>> Emergency approaching East road!\n");
        controller.reportEmergency("East");

        Thread.sleep(12000); // allow time for emergency handling
        controller.printStatus();

        // Simulate another emergency from South while East's emergency may still be active
        System.out.println("\n>>> Emergency approaching South road!\n");
        controller.reportEmergency("South");

        Thread.sleep(15000); // allow handling
        controller.printStatus();

        // Let normal operation continue for a bit
        Thread.sleep(8000);
        controller.printStatus();

        // Shutdown
        controller.stop();
    }
}
