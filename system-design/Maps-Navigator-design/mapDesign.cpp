#include<bits/stdc++.h>

using namespace std;

class Point {
public:
    double x, y;

    Point(double x, double y) : x(x), y(y) {}

    string toString() const {
        return "(" + to_string(x) + ", " + to_string(y) + ")";
    }
};

// Strategy Interface
class IRouteStrategy {
public:
    virtual vector<Point> buildPath(const Point& start, const Point& end) = 0;
    virtual string getName() = 0;
    virtual ~IRouteStrategy() = default;
};

class WalkRoute : public IRouteStrategy {
public:
    vector<Point> buildPath(const Point& start, const Point& end) override {
        cout << "Building walking path...\n";
        return { start, end };
    }

    string getName() override {
        return "Walking";
    }
};

// Concrete Strategy: Car
class CarRoute : public IRouteStrategy {
public:
    vector<Point> buildPath(const Point& start, const Point& end) override {
        cout << "Building car path with highways and signals...\n";
        return { start, Point((start.x + end.x)/2, (start.y + end.y)/2), end };
    }

    string getName() override {
        return "Car";
    }
};

// Concrete Strategy: Bus
class BusRoute : public IRouteStrategy {
public:
    vector<Point> buildPath(const Point& start, const Point& end) override {
        cout << "Building bus route with stops...\n";
        return { start, Point(start.x + 1, start.y + 2), end };
    }

    string getName() override {
        return "Bus";
    }
};

// Concrete Strategy: Bike
class BikeRoute : public IRouteStrategy {
public:
    vector<Point> buildPath(const Point& start, const Point& end) override {
        cout << "Building bike-friendly route...\n";
        return { start, Point((start.x + end.x) / 2, start.y), end };
    }

    string getName() override {
        return "Bike";
    }
};

// Context: Navigator Client
class NavigatorClient {
    shared_ptr<IRouteStrategy> routeStrategy;

public:
    void setStrategy(shared_ptr<IRouteStrategy> strategy) {
        routeStrategy = strategy;
    }

    void navigate(const Point& start, const Point& end) {
        if (!routeStrategy) {
            cout << "No transport strategy set!\n";
            return;
        }
        cout << "Using mode: " << routeStrategy->getName() << "\n";
        auto path = routeStrategy->buildPath(start, end);
        cout << "Generated Path:\n";
        for (const auto& point : path) {
            cout << point.toString() << " -> ";
        }
        cout << "END\n\n";
    }
};

int main() {
    NavigatorClient navigator;
    Point A(0, 0), B(10, 5);

    navigator.setStrategy(make_shared<WalkRoute>());
    navigator.navigate(A, B);

    navigator.setStrategy(make_shared<CarRoute>());
    navigator.navigate(A, B);

    navigator.setStrategy(make_shared<BusRoute>());
    navigator.navigate(A, B);

    navigator.setStrategy(make_shared<BikeRoute>());
    navigator.navigate(A, B);

    return 0;
}