import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

enum CarType {
    SUV,
    SEDAN,
    HATCHBACK
}

class DateRange {
    int start, end;

    DateRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    boolean overlaps(DateRange other) {
        return !(end <= other.start || start >= other.end);
    }
}

class Car {
    String id, model;
    CarType type;
    double pricePerDay;
    
    List<Reservation> reservations = new ArrayList<>();
    ReentrantLock lock = new ReentrantLock();
    
    Car(String id,String model, CarType type, double price) {
        this.id = id;
        this.model = model;
        this.type = type;
        this.pricePerDay = price;
    }
    
    boolean isAvailable(DateRange range) {
        for (Reservation r : reservations) {
            if (r.dateRange.overlaps(range)) return false;
        }
        return true;
    }
}

class Customer {
    String id, name, license;
    
    Customer(String id, String name, String license) {
        this.id = id;
        this.name = name;
        this.license = license;
    }
}

class Reservation {
    String id;
    Customer customer;
    Car car;
    DateRange dateRange;
    double amount;

    Reservation(String id, Customer c, Car car, DateRange range, double amount) {
        this.id = id;
        this.customer = c;
        this.car = car;
        this.dateRange = range;
        this.amount = amount;
    }
}

/* ================= PAYMENT STRATEGY ================= */
interface PaymentStrategy {
    void pay(double amount);
}

class CardPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + " via Card");
    }
}

class UpiPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + " via UPI");
    }
}

class CarRentalService {
    Map<String, Car> cars = new HashMap<>();
    Map<String, Reservation> reservations = new HashMap<>();
    
    List<Car> search(CarType type, double maxPrice, DateRange range) {
        List<Car> result = new ArrayList<>();
        
        for(Car car : cars.values()) {
            if(car.type == type && 
                car.pricePerDay <= maxPrice && 
                car.isAvailable(range)) {
                    result.add(car);
                }
        }
        return result;
    }
    
    /* RESERVE */
    Reservation reserve(String customerId, Customer customer,
                        String carId, DateRange range,
                        PaymentStrategy payment) {

        Car car = cars.get(carId);

        car.lock.lock();
        try {
            if (!car.isAvailable(range)) {
                throw new RuntimeException("Car not available");
            }

            int days = range.end - range.start;
            double amount = days * car.pricePerDay;

            Reservation r = new Reservation(
                    UUID.randomUUID().toString(),
                    customer,
                    car,
                    range,
                    amount
            );

            car.reservations.add(r);
            reservations.put(r.id, r);

            payment.pay(amount);

            return r;

        } finally {
            car.lock.unlock();
        }
    }

    /* CANCEL */
    void cancel(String reservationId) {
        Reservation r = reservations.remove(reservationId);
        r.car.reservations.remove(r);
    }
}

public class Main {
    public static void main(String[] args) {

        CarRentalService service = new CarRentalService();

        Car car = new Car("C1", "Swift", CarType.HATCHBACK, 1000);
        service.cars.put("C1", car);

        Customer customer = new Customer("U1", "Sachin", "DL123");

        DateRange range = new DateRange(1, 5);

        Reservation r = service.reserve(
                "U1",
                customer,
                "C1",
                range,
                new CardPayment()
        );

        System.out.println("Reserved: " + r.id);
    }
}

