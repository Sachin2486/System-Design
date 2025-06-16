#include<bits/stdc++.h>
#include <iostream>

using namespace std;
using namespace chrono;

enum class SignalColor { RED, YELLOW, GREEN };

string toString(SignalColor color) {
    switch(color) {
        case SignalColor::RED: return "RED";
        case SignalColor::YELLOW: return "YELLOW";
        case SignalColor::GREEN: return "GREEN";
    }
    return "UNKNOWN";
}

class Signal {
    SignalColor current;
    map<SignalColor, int> durations; //in seconds
    
    public:
    Signal(int redDuration, int yellowDuration, int greenDuration) {
        durations[SignalColor::RED] = redDuration;
        durations[SignalColor::YELLOW] = yellowDuration;
        durations[SignalColor::GREEN] = greenDuration;
        current = SignalColor::RED;
    }
    
    SignalColor getCurrent() {
        return current;
    }
    
    void next() {
        switch(current) {
            case SignalColor::RED: current = SignalColor::GREEN; break;
            case SignalColor::GREEN: current = SignalColor::YELLOW; break;
            case SignalColor::YELLOW: current = SignalColor::RED; break;
        }
    }
    
    int getDuration() {
        return durations[current];
    }
    
    void overrideToGreen() {
        current = SignalColor::GREEN;
    }
    
    void printStatus(const string& road) {
        cout << "Road " << road << " Signal: " << toString(current) << endl;
    }
};

class IntersectionController {
    map<string, Signal> signals;
    atomic<bool> emergencyDetected{false};
    mutex mtx;
    
    public:
    void addRoad(const string& road, const Signal& signal) {
    signals.emplace(road, signal);
}
    
    void startSimulation() {
        thread([this]() {
            while (true) {
                for (auto& [road, signal] : signals) {
                    unique_lock<mutex> lock(mtx);
                    if (emergencyDetected) {
                        signal.overrideToGreen();
                        signal.printStatus(road);
                        this_thread::sleep_for(seconds(5));
                        emergencyDetected = false;
                        continue;
                    }

                    signal.printStatus(road);
                    lock.unlock();
                    this_thread::sleep_for(seconds(signal.getDuration()));
                    lock.lock();
                    signal.next();
                }
            }
        }).detach();
    }
    
    void detectEmergency(const string& road) {
        lock_guard<mutex> lock(mtx);
        cout << "[EMERGENCY] Emergency vehicle detected on road: " << road << " - overriding to GREEN\n";
        emergencyDetected = true;
    }
};

int main() {
    IntersectionController controller;
    controller.addRoad("A", Signal(5, 2, 5));
    controller.addRoad("B", Signal(5, 2, 5));

    controller.startSimulation();

    // Simulate emergency
    this_thread::sleep_for(seconds(10));
    controller.detectEmergency("A");

    // Keep program running
    this_thread::sleep_for(seconds(30));
    return 0;
}