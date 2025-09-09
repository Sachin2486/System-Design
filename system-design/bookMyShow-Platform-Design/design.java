// Movie
// title, duration, genre

// Theater
// name, location, list of shows

// Show
// movie, theater, timing, seating arrangement

// Seat
// seatId, type (Normal/Premium), price, availability

// User
// id, name, email

// Booking
// user, show, seats, payment status

// Payment
// amount, status

// Admin
// can add/remove/update movies, shows, seating

// BookingService (Facade)
// manages browsing movies, booking tickets, payments, concurrency control

import java.util.*;
import java.util.concurrent.locks.*;

// Movie ///
class Movie {
    private String title;
    private int duration;
    private String genre;
    
    public Movie(String title, int duration, String genre) {
        this.title = title;
        this.duration = duration;
        this.genre = genre;
    }
    
    public String getTitle() {
        return title;
    }
    
}

// --- Seat ---
enum SeatType {
    NORMAL, 
    PREMIUM
}

class Seat {
    private String seatId;
    private SeatType type;
    private double price;
    private boolean booked;
    private Lock lock; // for concurrency
    
    public Seat(String seatId, SeatType type, double price) {
        this.seatId = seatId;
        this.type = type;
        this.price = price;
        this.booked = false;
        this.lock = new ReentrantLock();
    }
    
    public String getSeatId() {
        return seatId;
    }
    
    public double getPrice() { return price; }
    public boolean isBooked() { return booked; }
    
    public boolean book() {
        lock.lock();
        try {
            if(!booked) {
                booked = true;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}

class Show {
    private Movie movie;
    private String time;
    private List<Seat> seats;
    
    public Show(Movie movie, String time, int normalSeats, int premiumSeats) {
        this.movie = movie;
        this.time = time;
        this.seats = new ArrayList<>();
        
        // create seating arrangement
        for (int i = 1; i <= normalSeats; i++) {
            seats.add(new Seat("N" + i, SeatType.NORMAL, 200));
        }
        for (int i = 1; i <= premiumSeats; i++) {
            seats.add(new Seat("P" + i, SeatType.PREMIUM, 400));
        }
    }
    
    public Movie getMovie() {
        return movie;
    }
    
    public String getTime() {
        return time;
    }
    
    public List<Seat> getSeats() {
        return seats;
    }
    
    public void showAvailableSeats() {
        for (Seat s : seats) {
            if (!s.isBooked()) {
                System.out.print(s.getSeatId() + "(" + s.getPrice() + ") ");
            }
        }
        System.out.println();
    }
}

// --- Theater ---
class Theater {
    private String name;
    private String location;
    private List<Show> shows;

    public Theater(String name, String location) {
        this.name = name;
        this.location = location;
        this.shows = new ArrayList<>();
    }

    public void addShow(Show show) { shows.add(show); }
    public List<Show> getShows() { return shows; }
    public String getName() { return name; }
}

class User {
    private String id;
    private String name;
    private String email;
    
    User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
}

// --- Booking ---
class Booking {
    private User user;
    private Show show;
    private List<Seat> bookedSeats;
    private double totalAmount;
    private boolean paymentDone;

    public Booking(User user, Show show, List<Seat> bookedSeats) {
        this.user = user;
        this.show = show;
        this.bookedSeats = bookedSeats;
        this.totalAmount = bookedSeats.stream().mapToDouble(Seat::getPrice).sum();
        this.paymentDone = false;
    }

    public void makePayment() {
        // simulate payment
        System.out.println("Processing payment of ₹" + totalAmount + " for " + user.getName());
        this.paymentDone = true;
        System.out.println("Payment successful. Booking confirmed!");
    }
}

class BookingService {
    public Booking bookSeats(User user, Show show, List<String> seatIds) {
        List<Seat> booked = new ArrayList<>();
        for (Seat seat : show.getSeats()) {
            if (seatIds.contains(seat.getSeatId())) {
                if (seat.book()) {
                    booked.add(seat);
                } else {
                    System.out.println("Seat " + seat.getSeatId() + " already booked!");
                }
            }
        }

        if (!booked.isEmpty()) {
            Booking booking = new Booking(user, show, booked);
            booking.makePayment();
            return booking;
        } else {
            System.out.println("Booking failed, no seats available.");
            return null;
        }
    }
}

public class MovieBookingDemo {
    public static void main (String[] args) {
        // Movies
        Movie m1 = new Movie("Inception", 148, "Sci-Fi");
        Movie m2 = new Movie("Interstellar", 169, "Sci-Fi");

        // Theater
        Theater t1 = new Theater("PVR Cinemas", "Mumbai");

        // Shows
        Show s1 = new Show(m1, "6:00 PM", 5, 2);
        Show s2 = new Show(m2, "9:00 PM", 5, 2);

        t1.addShow(s1);
        t1.addShow(s2);

        // Users
        User u1 = new User("U1", "Alice", "alice@mail.com");
        User u2 = new User("U2", "Bob", "bob@mail.com");

        // Booking Service
        BookingService bookingService = new BookingService();
        
        System.out.println("\nAvailable seats for Inception:");
        s1.showAvailableSeats();

        // Alice books 2 seats
        bookingService.bookSeats(u1, s1, Arrays.asList("N1", "P1"));

        System.out.println("\nAvailable seats after Alice's booking:");
        s1.showAvailableSeats();

        // Bob tries to book same seat + another seat
        bookingService.bookSeats(u2, s1, Arrays.asList("P1", "N2"));

        System.out.println("\nAvailable seats after Bob's attempt:");
        s1.showAvailableSeats();
    }
}

