import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

enum SeatType {
    NORMAL, PREMIUM
}

enum SeatStatus {
    AVAILABLE, BOOKED
}

enum PaymentStatus {
    PENDING, SUCCESS, FAILED
}

class User {
    private final int id;
    private final String name;
    
    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public  String getName() {
        return name;
    }
}

class Movie {
    private final int id;
    private final String title;
    private final int durationMinutes;
    
    public Movie(int id, String title, int durationMinutes) {
        this.id = id;
        this.title = title;
        this.durationMinutes = durationMinutes;
    }
    
    public int getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }
    
    @Override
    public String toString() {
        return "Movie{id=" + id + ", title='" + title + "', duration=" + durationMinutes + " mins}";
    }
}

class Seat {
    private final int id;
    private final String label; // e.g. A1, A2
    private final SeatType type;
    private final double price;
    private SeatStatus status;

    public Seat(int id, String label, SeatType type, double price) {
        this.id = id;
        this.label = label;
        this.type = type;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    public int getId() { return id; }
    public String getLabel() { return label; }
    public SeatType getType() { return type; }
    public double getPrice() { return price; }
    public SeatStatus getStatus() { return status; }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return label + "(" + type + "," + status + "," + price + ")";
    }
}

class Theater {
    private final int id;
    private final String name;
    private final String location;
    
    public Theater(int id, String name, String location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getLocation() {
        return location;
    }
    
    @Override
    public String toString() {
        return "Theater{id=" + id + ", name='" + name + "', location='" + location + "'}";
    }
}

class Show {
    private final int id;
    private final Movie movie;
    private final Theater theater;
    private final Date showTime;
    private final Map<Integer, Seat> seats = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Show(int id, Movie movie, Theater theater, Date showTime) {
        this.id = id;
        this.movie = movie;
        this.theater = theater;
        this.showTime = showTime;
    }

    public int getId() { return id; }
    public Movie getMovie() { return movie; }
    public Theater getTheater() { return theater; }
    public Date getShowTime() { return showTime; }

    public void addSeat(Seat seat) {
        seats.put(seat.getId(), seat);
    }

    public Collection<Seat> getAllSeats() {
        return seats.values();
    }

    public void printSeatingLayout() {
        System.out.println("\nSeating layout for Show " + id + " (" +
                movie.getTitle() + " @ " + theater.getName() + "):");

        // Simple print: group by rows (first letter of label)
        Map<Character, List<Seat>> byRow = new TreeMap<>();
        for (Seat s : seats.values()) {
            char row = s.getLabel().charAt(0);
            byRow.putIfAbsent(row, new ArrayList<>());
            byRow.get(row).add(s);
        }
        for (Map.Entry<Character, List<Seat>> e : byRow.entrySet()) {
            System.out.print("Row " + e.getKey() + ": ");
            e.getValue().stream()
                    .sorted(Comparator.comparing(Seat::getLabel))
                    .forEach(seat -> System.out.print(seat.getLabel() + "[" + seat.getStatus() + "] "));
            System.out.println();
        }
    }

    /**
     * Thread-safe seat booking: ensures no double booking.
     */
    public List<Seat> bookSeats(List<Integer> seatIds) {
        lock.lock();
        try {
            List<Seat> selected = new ArrayList<>();
            // Check availability first
            for (int seatId : seatIds) {
                Seat seat = seats.get(seatId);
                if (seat == null || seat.getStatus() == SeatStatus.BOOKED) {
                    return null; // at least one seat not available
                }
                selected.add(seat);
            }
            // Mark as booked
            for (Seat s : selected) {
                s.setStatus(SeatStatus.BOOKED);
            }
            return selected;
        } finally {
            lock.unlock();
        }
    }
}

class Booking {
    private final int id;
    private final User user;
    private final Show show;
    private final List<Seat> seats;
    private final double totalAmount;
    private PaymentStatus paymentStatus;

    public Booking(int id, User user, Show show, List<Seat> seats, double totalAmount) {
        this.id = id;
        this.user = user;
        this.show = show;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.paymentStatus = PaymentStatus.PENDING;
    }

    public int getId() { return id; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }

    public void markPaymentStatus(PaymentStatus status) {
        this.paymentStatus = status;
    }

    @Override
    public String toString() {
        return "Booking{id=" + id +
                ", user=" + user.getName() +
                ", movie=" + show.getMovie().getTitle() +
                ", theater=" + show.getTheater().getName() +
                ", showTime=" + show.getShowTime() +
                ", seats=" + seats +
                ", totalAmount=" + totalAmount +
                ", paymentStatus=" + paymentStatus +
                '}';
    }
}

class PaymentService {
    // For interview: simple simulation
    public boolean processPayment(User user, double amount) {
        System.out.println("[PAYMENT] Charging " + user.getName() + " amount: " + amount);
        // Always succeed in this demo
        return true;
    }
}

class MovieBookingSystem {

    private final Map<Integer, Movie> movies = new ConcurrentHashMap<>();
    private final Map<Integer, Theater> theaters = new ConcurrentHashMap<>();
    private final Map<Integer, Show> shows = new ConcurrentHashMap<>();
    private final Map<Integer, Booking> bookings = new ConcurrentHashMap<>();

    private final AtomicInteger movieIdGen = new AtomicInteger(1);
    private final AtomicInteger theaterIdGen = new AtomicInteger(1);
    private final AtomicInteger showIdGen = new AtomicInteger(1);
    private final AtomicInteger bookingIdGen = new AtomicInteger(1);

    private final PaymentService paymentService = new PaymentService();

    /* ===== Admin Operations ===== */

    public Movie addMovie(String title, int duration) {
        int id = movieIdGen.getAndIncrement();
        Movie m = new Movie(id, title, duration);
        movies.put(id, m);
        System.out.println("[ADMIN] Added movie: " + m);
        return m;
    }

    public Theater addTheater(String name, String location) {
        int id = theaterIdGen.getAndIncrement();
        Theater t = new Theater(id, name, location);
        theaters.put(id, t);
        System.out.println("[ADMIN] Added theater: " + t);
        return t;
    }

    public Show addShow(int movieId, int theaterId, Date showTime) {
        Movie m = movies.get(movieId);
        Theater t = theaters.get(theaterId);
        if (m == null || t == null) {
            throw new IllegalArgumentException("Invalid movie or theater id");
        }
        int id = showIdGen.getAndIncrement();
        Show s = new Show(id, m, t, showTime);
        shows.put(id, s);
        System.out.println("[ADMIN] Added show: " + id + " for movie " + m.getTitle() +
                " at theater " + t.getName() + " time " + showTime);
        return s;
    }

    public void configureSeatsForShow(int showId, int rows, int cols,
                                      int premiumRows, double normalPrice, double premiumPrice) {
        Show show = shows.get(showId);
        if (show == null) {
            throw new IllegalArgumentException("Show not found");
        }
        int seatId = 1;
        for (int r = 0; r < rows; r++) {
            char rowChar = (char) ('A' + r);
            for (int c = 1; c <= cols; c++) {
                String label = rowChar + String.valueOf(c);
                SeatType type = (r < premiumRows) ? SeatType.PREMIUM : SeatType.NORMAL;
                double price = (type == SeatType.PREMIUM) ? premiumPrice : normalPrice;
                Seat seat = new Seat(seatId++, label, type, price);
                show.addSeat(seat);
            }
        }
        System.out.println("[ADMIN] Configured seats for show " + showId);
    }

    /* ===== User Operations ===== */

    public List<Movie> listMovies() {
        return new ArrayList<>(movies.values());
    }

    public List<Show> listShowsByMovie(int movieId) {
        List<Show> result = new ArrayList<>();
        for (Show s : shows.values()) {
            if (s.getMovie().getId() == movieId) {
                result.add(s);
            }
        }
        return result;
    }

    public void viewSeating(int showId) {
        Show show = shows.get(showId);
        if (show != null) {
            show.printSeatingLayout();
        }
    }

    public Booking bookTickets(User user, int showId, List<Integer> seatIds) {
        Show show = shows.get(showId);
        if (show == null) {
            System.out.println("[BOOKING] Show not found");
            return null;
        }

        // Concurrency handled inside Show.bookSeats()
        List<Seat> reserved = show.bookSeats(seatIds);
        if (reserved == null) {
            System.out.println("[BOOKING] Some seats are already booked, booking failed for user " + user.getName());
            return null;
        }

        double totalAmount = reserved.stream().mapToDouble(Seat::getPrice).sum();
        int bookingId = bookingIdGen.getAndIncrement();
        Booking booking = new Booking(bookingId, user, show, reserved, totalAmount);
        bookings.put(bookingId, booking);

        boolean paymentSuccess = paymentService.processPayment(user, totalAmount);
        booking.markPaymentStatus(paymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        System.out.println("[BOOKING] Booking created: " + booking);
        return booking;
    }
}

public class MovieBookingDemo {
    public static void main(String[] args) {

        MovieBookingSystem system = new MovieBookingSystem();

        // Admin: Add movies and theaters
        Movie m1 = system.addMovie("Inception", 148);
        Movie m2 = system.addMovie("Interstellar", 169);

        Theater t1 = system.addTheater("PVR Koramangala", "Bangalore");
        Theater t2 = system.addTheater("INOX Mall", "Bangalore");

        // Admin: Add shows
        Show s1 = system.addShow(m1.getId(), t1.getId(), new Date());
        Show s2 = system.addShow(m2.getId(), t2.getId(), new Date(System.currentTimeMillis() + 3600_000)); // +1 hr

        // Admin: Configure seats (rows, cols, premiumRows, normalPrice, premiumPrice)
        system.configureSeatsForShow(s1.getId(), 5, 10, 2, 200.0, 300.0);
        system.configureSeatsForShow(s2.getId(), 4, 8, 1, 180.0, 280.0);

        // Users
        User sachin = new User(1, "Sachin");
        User rohit = new User(2, "Rohit");

        // List movies
        System.out.println("\n=== Movies Available ===");
        for (Movie m : system.listMovies()) {
            System.out.println(m);
        }

        // List shows for a movie
        System.out.println("\n=== Shows for " + m1.getTitle() + " ===");
        for (Show s : system.listShowsByMovie(m1.getId())) {
            System.out.println("Show ID: " + s.getId() + ", Theater: " + s.getTheater().getName()
                    + ", Time: " + s.getShowTime());
        }

        // View seating for show s1
        system.viewSeating(s1.getId());

        // Book tickets
        List<Integer> seatsForSachin = Arrays.asList(1, 2, 3); // seat IDs
        system.bookTickets(sachin, s1.getId(), seatsForSachin);

        // Try conflicting booking (concurrent-like scenario)
        List<Integer> seatsForRohit = Arrays.asList(2, 4);
        system.bookTickets(rohit, s1.getId(), seatsForRohit);

        // View seating again to see updated status
        system.viewSeating(s1.getId());
    }
}