#include<bits/stdc++.h>
#include <iostream>

using namespace std;

class Location {
    public:
    int pointId;
    Location(int id) : pointId(id) {}
};

class Passenger {
   public:
   int id;
   string name;
   Location start;
   Location end;
   int startTime;
   int endTime;
   
   Passenger(int id, string name, Location start, Location end, int startTime, int endTime)
      : id(id), name(name), start(start), end(end), startTime(startTime), endTime(endTime) {}
};

class FareStrategy {
    public:
    virtual double calculate(const Passenger& p) const = 0;
    virtual ~FareStrategy() = default;
};

class DistanceFareStrategy : public FareStrategy {
    double ratePerKm;
    
    public:
    DistanceFareStrategy(double rate) : ratePerKm(rate) {}
    
    double calculate(const Passenger& p) const override {
        int distance = abs(p.end.pointId - p.start.pointId);
        return ratePerKm * distance;
    }
};

class TimeFareStrategy : public FareStrategy {
    double ratePerMinute;
public:
    TimeFareStrategy(double rate) : ratePerMinute(rate) {}

    double calculate(const Passenger& p) const override {
        int duration = p.endTime - p.startTime;
        return ratePerMinute * duration;
    }
};

class FareCalculator {
  vector<shared_ptr<FareStrategy>> strategies;
  
  public:
  void addStrategy(shared_ptr<FareStrategy> strategy)
  {
      strategies.push_back(strategy);
  }
  
  double calculateFare(const Passenger& p) const {
      double totalFare = 0;
      for(const auto& strategy : strategies) {
          totalFare += strategy->calculate(p);
      }
      return totalFare;
  }
};

class Ride {
    int rideId;
    vector <Passenger> passengers;
    FareCalculator fareCalculator;
    
    public:
    Ride(int id) : rideId(id) {}
    
    void setFareCalculator(const FareCalculator& fc) {
        fareCalculator = fc;
    }

    void addPassenger(const Passenger& p) {
        passengers.push_back(p);
    }
    
    void endRideAndCalculateFare() {
        std::cout << "Fare breakdown for Ride #" << rideId << ":\n";
        for (const auto& p : passengers) {
            double fare = fareCalculator.calculateFare(p);
            std::cout << "Passenger: " << p.name << ", Fare: ₹" << fare << "\n";
        }
    }
};


int main()
{
    Ride ride(101);
    
    Passenger p1(1, "Sachin", Location(0), Location(10), 0,30);
    Passenger p2(2, "Bob", Location(5), Location(15), 10, 40);
    
    ride.addPassenger(p1);
    ride.addPassenger(p2);
    
    FareCalculator calculator;
    calculator.addStrategy(std::make_shared<DistanceFareStrategy>(10));
    calculator.addStrategy(std::make_shared<TimeFareStrategy>(2));
    
    ride.setFareCalculator(calculator);
    
    ride.endRideAndCalculateFare();

    return 0;
}