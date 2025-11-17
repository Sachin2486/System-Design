import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/* Signal colors */
enum SignalColor { RED, YELLOW, GREEN }

/* Represents a traffic signal for one road */
class Signal {
    final String id;
    private volatile SignalColor color;
    private volatile int greenDurationSec;
    private volatile int yellowDurationSec;

    Signal(String id, int greenSec, int yellowSec) {
        this.id = id;
        this.greenDurationSec = Math.max(1, greenSec);
        this.yellowDurationSec = Math.max(1, yellowSec);
        this.color = SignalColor.RED;
    }

    void setColor(SignalColor c) {
        this.color = c;
        log("Signal " + id + " -> " + c);
    }

    SignalColor getColor() { return color; }

    int getGreenDuration() { return greenDurationSec; }
    int getYellowDuration() { return yellowDurationSec; }

    void setGreenDuration(int sec) { this.greenDurationSec = Math.max(1, sec); }
    void setYellowDuration(int sec) { this.yellowDurationSec = Math.max(1, sec); }

    private void log(String s){
        System.out.printf("[%s] %s%n", LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME), s);
    }
}

/* Orchestrates signals; handles cycles and emergency preemption */
class TrafficController {
    private final List<Signal> cycleOrder;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ReentrantLock lock = new ReentrantLock();

    private volatile int currentIndex = 0;
    private volatile boolean running = false;

    private volatile boolean emergencyMode = false;
    private volatile String emergencyRoadId = null;
    private volatile ScheduledFuture<?> scheduledTask = null;

    TrafficController(List<Signal> cycleOrder) {
        if (cycleOrder == null || cycleOrder.isEmpty()) throw new IllegalArgumentException("Cycle must have at least one signal");
        this.cycleOrder = new ArrayList<>(cycleOrder);
    }

    /* Start the normal cycling */
    void start() {
        lock.lock();
        try {
            if (running) return;
            running = true;
            for (Signal s : cycleOrder) s.setColor(SignalColor.RED);
            scheduleNextCycle(0); // immediate start
            log("Controller started");
        } finally { lock.unlock(); }
    }

    /* Stop the controller and scheduler */
    void stop() {
        lock.lock();
        try {
            running = false;
            if (scheduledTask != null) scheduledTask.cancel(true);
            scheduler.shutdownNow();
            log("Controller stopped");
        } finally { lock.unlock(); }
    }

    /* Trigger emergency: preempt normal cycle and give GREEN to emergencyRoadId for durationSec seconds */
    void triggerEmergency(String roadId, int durationSec) {
        lock.lock();
        try {
            log("EMERGENCY request for " + roadId + " duration " + durationSec + "s");
            emergencyMode = true;
            emergencyRoadId = roadId;
            if (scheduledTask != null) scheduledTask.cancel(true);
            preemptForEmergency(roadId, Math.max(1, durationSec));
        } finally { lock.unlock(); }
    }

    /* Clear emergency and resume normal cycle */
    void clearEmergency() {
        lock.lock();
        try {
            if (!emergencyMode) return;
            log("Clearing EMERGENCY for " + emergencyRoadId);
            emergencyMode = false;
            emergencyRoadId = null;
            if (scheduledTask != null) scheduledTask.cancel(true);
            // resume from next index after the emergency road if it is in cycle; otherwise continue currentIndex
            currentIndex = Math.max(0, currentIndex % cycleOrder.size());
            scheduleNextCycle(0);
        } finally { lock.unlock(); }
    }

    /* Adjust durations at runtime for a specific signal */
    void adjustDurations(String roadId, int greenSec, int yellowSec) {
        for (Signal s : cycleOrder) {
            if (s.id.equals(roadId)) {
                s.setGreenDuration(greenSec);
                s.setYellowDuration(yellowSec);
                log("Adjusted durations for " + roadId + " -> green:" + greenSec + "s yellow:" + yellowSec + "s");
                return;
            }
        }
        log("No signal found with id " + roadId);
    }

    /* Internal: schedule normal cycle starting after delaySec seconds */
    private void scheduleNextCycle(int delaySec) {
        scheduledTask = scheduler.schedule(() -> {
            lock.lock();
            try {
                if (!running || emergencyMode) return;
                Signal current = cycleOrder.get(currentIndex);
                // green
                current.setColor(SignalColor.GREEN);
                int g = current.getGreenDuration();
                scheduledTask = scheduler.schedule(() -> {
                    lock.lock();
                    try {
                        // yellow
                        current.setColor(SignalColor.YELLOW);
                        int y = current.getYellowDuration();
                        scheduledTask = scheduler.schedule(() -> {
                            lock.lock();
                            try {
                                current.setColor(SignalColor.RED);
                                // advance to next signal
                                currentIndex = (currentIndex + 1) % cycleOrder.size();
                                scheduleNextCycle(0);
                            } finally { lock.unlock(); }
                        }, y, TimeUnit.SECONDS);
                    } finally { lock.unlock(); }
                }, g, TimeUnit.SECONDS);
            } finally { lock.unlock(); }
        }, delaySec, TimeUnit.SECONDS);
    }

    /* Internal: immediately preempt to handle emergency road */
    private void preemptForEmergency(String roadId, int durationSec) {
        // set all to RED, then set emergency road GREEN for duration, then return to normal flow
        for (Signal s : cycleOrder) s.setColor(SignalColor.RED);
        Optional<Signal> opt = cycleOrder.stream().filter(s -> s.id.equals(roadId)).findFirst();
        if (opt.isEmpty()) {
            log("Emergency road " + roadId + " not in cycle. Keeping all RED.");
            // schedule resume after duration
            scheduledTask = scheduler.schedule(() -> {
                lock.lock();
                try { clearEmergency(); } finally { lock.unlock(); }
            }, durationSec, TimeUnit.SECONDS);
            return;
        }

        Signal emergencySignal = opt.get();
        emergencySignal.setColor(SignalColor.GREEN);

        scheduledTask = scheduler.schedule(() -> {
            lock.lock();
            try {
                // safety: transition to yellow then red before resuming normal operations
                emergencySignal.setColor(SignalColor.YELLOW);
                int y = emergencySignal.getYellowDuration();
                scheduledTask = scheduler.schedule(() -> {
                    lock.lock();
                    try {
                        emergencySignal.setColor(SignalColor.RED);
                        // after emergency we clear the emergency and resume cycle from the next signal in order
                        int idx = cycleOrder.indexOf(emergencySignal);
                        currentIndex = (idx + 1) % cycleOrder.size();
                        emergencyMode = false;
                        emergencyRoadId = null;
                        scheduleNextCycle(0);
                    } finally { lock.unlock(); }
                }, y, TimeUnit.SECONDS);
            } finally { lock.unlock(); }
        }, durationSec, TimeUnit.SECONDS);
    }

    private void log(String s){
        System.out.printf("[%s] %s%n", LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME), s);
    }
}

/* Demo: create a 4-way intersection controller and simulate an emergency */
public class TrafficSignalDemo {
    public static void main(String[] args) throws Exception {
        Signal north = new Signal("North", 6, 2);
        Signal east  = new Signal("East", 5, 2);
        Signal south = new Signal("South", 6, 2);
        Signal west  = new Signal("West", 4, 2);

        TrafficController controller = new TrafficController(Arrays.asList(north, east, south, west));
        controller.start();

        // After 15 seconds adjust durations (simulating adaptive timing)
        ScheduledExecutorService sim = Executors.newSingleThreadScheduledExecutor();
        sim.schedule(() -> controller.adjustDurations("West", 8, 2), 15, TimeUnit.SECONDS);

        // After 25 seconds simulate an emergency from South for 8 seconds
        sim.schedule(() -> controller.triggerEmergency("South", 8), 25, TimeUnit.SECONDS);

        // After 60 seconds stop demo
        sim.schedule(() -> {
            controller.stop();
            sim.shutdownNow();
        }, 60, TimeUnit.SECONDS);
    }
}
