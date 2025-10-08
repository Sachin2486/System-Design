import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Movie Ticket Booking - compact, interview-ready implementation.
 * Run: javac MovieBookingApp.java && java MovieBookingApp
 */
public class MovieBookingApp {

    // ----------------- Enums -----------------
    enum SeatType { NORMAL, PREMIUM }

    enum BookingStatus { SUCCESS, FAILED }

    // ----------------- Domain Models -----------------
    static class Seat {
        final String id; // e.g., "A1"
        final SeatType type;
        final double price;
        final AtomicBoolean booked = new AtomicBoolean(false);

        Seat(String id, SeatType type, double price) {
            this.id = id;
            this.type = type;
            this.price = price;
        }

        boolean tryReserve() {
            return booked.compareAndSet(false, true);
        }

        void release() {
            booked.set(false);
        }

        boolean isBooked() {
            return booked.get();
        }

        @Override public String toString() {
            return id + "(" + type + (isBooked() ? ":X" : ":O") + ")";
        }
    }

    static class Theater {
        final String id;
        final String name;
        final Map<String, Show> shows = new ConcurrentHashMap<>();

        Theater(String id, String name) { this.id = id; this.name = name; }

        void addShow(Show s) { shows.put(s.id, s); }
        void removeShow(String showId) { shows.remove(showId); }
    }

    static class Movie {
        final String id;
        final String title;
        final int durationMinutes;

        Movie(String id, String title, int durationMinutes) {
            this.id = id; this.title = title; this.durationMinutes = durationMinutes;
        }
    }

    static class Show {
        final String id; // unique show id
        final Movie movie;
        final Theater theater;
        final LocalDateTime startTime;
        final Map<String, Seat> seats; // seatId -> Seat

        Show(String id, Movie movie, Theater theater, LocalDateTime startTime, Map<String, Seat> seats) {
            this.id = id; this.movie = movie; this.theater = theater; this.startTime = startTime;
            this.seats = new ConcurrentHashMap<>(seats);
        }

        // snapshot of seating
        Map<String, Boolean> seatingSnapshot() {
            return seats.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().isBooked()));
        }

        void printSeating() {
            System.out.println("Seating for show " + id + " - " + movie.title + " @ " + startTime);
            seats.values().forEach(s -> System.out.print(s + " "));
            System.out.println();
        }
    }

    static class User {
        final String id;
        final String name;
        User(String id, String name) { this.id = id; this.name = name; }
    }

    static class Booking {
        final String bookingId;
        final String showId;
        final String userId;
        final List<String> seats;
        final double amount;
        final LocalDateTime createdAt;

        Booking(String bookingId, String showId, String userId, List<String> seats, double amount) {
            this.bookingId = bookingId; this.showId = showId; this.userId = userId; this.seats = seats;
            this.amount = amount; this.createdAt = LocalDateTime.now();
        }
    }

    // ----------------- Services & Repositories -----------------
    static class InMemoryRepo {
        final Map<String, Theater> theaters = new ConcurrentHashMap<>();
        final Map<String, Movie> movies = new ConcurrentHashMap<>();
        final Map<String, Show> shows = new ConcurrentHashMap<>();
        final Map<String, User> users = new ConcurrentHashMap<>();
        final Map<String, Booking> bookings = new ConcurrentHashMap<>();
    }

    static class PaymentService {
        // Mock payment - always succeed in this demo
        boolean charge(String userId, double amount) {
            // In real: integrate with payment gateway; handle retries, idempotency.
            return true;
        }
    }

    static class BookingService {
        private final InMemoryRepo repo;
        private final PaymentService paymentService;
        private final AtomicLong bookingCounter = new AtomicLong(1000);

        BookingService(InMemoryRepo repo, PaymentService paymentService) {
            this.repo = repo; this.paymentService = paymentService;
        }

        // Attempt to book seats atomically using seat.tryReserve()
        synchronized BookingResult bookSeats(String userId, String showId, List<String> seatIds) {
            Show show = repo.shows.get(showId);
            if (show == null) return BookingResult.failed("Show not found");

            List<Seat> seatsToReserve = new ArrayList<>();
            for (String sid : seatIds) {
                Seat s = show.seats.get(sid);
                if (s == null) return BookingResult.failed("Seat " + sid + " not found");
                seatsToReserve.add(s);
            }

            // Attempt to reserve all seats atomically (optimistic)
            List<Seat> reserved = new ArrayList<>();
            for (Seat s : seatsToReserve) {
                boolean ok = s.tryReserve();
                if (!ok) {
                    // release already reserved in this attempt
                    for (Seat r : reserved) r.release();
                    return BookingResult.failed("Seat " + s.id + " already booked");
                }
                reserved.add(s);
            }

            // compute amount
            double amount = reserved.stream().mapToDouble(seat -> seat.price).sum();

            // payment
            boolean paid = paymentService.charge(userId, amount);
            if (!paid) {
                // rollback
                for (Seat r : reserved) r.release();
                return BookingResult.failed("Payment failed");
            }

            // create booking record
            String bid = "BKG-" + bookingCounter.getAndIncrement();
            Booking booking = new Booking(bid, showId, userId, seatIds, amount);
            repo.bookings.put(bid, booking);
            return BookingResult.success(booking);
        }
    }

    static class BookingResult {
        final BookingStatus status;
        final String message;
        final Booking booking;

        private BookingResult(BookingStatus status, String message, Booking booking) {
            this.status = status; this.message = message; this.booking = booking;
        }

        static BookingResult success(Booking b) {
            return new BookingResult(BookingStatus.SUCCESS, "Booked: " + b.bookingId, b);
        }

        static BookingResult failed(String reason) {
            return new BookingResult(BookingStatus.FAILED, reason, null);
        }
    }

    // ----------------- Admin Service -----------------
    static class AdminService {
        final InMemoryRepo repo;

        AdminService(InMemoryRepo repo) { this.repo = repo; }

        Theater createTheater(String id, String name) {
            Theater t = new Theater(id, name); repo.theaters.put(id, t); return t;
        }

        Movie createMovie(String id, String title, int duration) {
            Movie m = new Movie(id, title, duration); repo.movies.put(id, m); return m;
        }

        Show createShow(String showId, String movieId, String theaterId, LocalDateTime start,
                        Map<String, Seat> seats) {
            Movie m = repo.movies.get(movieId);
            Theater t = repo.theaters.get(theaterId);
            if (m == null || t == null) throw new IllegalArgumentException("movie/theater missing");
            Show s = new Show(showId, m, t, start, seats);
            t.addShow(s);
            repo.shows.put(showId, s);
            return s;
        }

        void removeShow(String showId) {
            Show s = repo.shows.remove(showId);
            if (s != null) s.theater.removeShow(showId);
        }
    }

    // ----------------- Demo / Main -----------------
    public static void main(String[] args) throws Exception {
        InMemoryRepo repo = new InMemoryRepo();
        AdminService admin = new AdminService(repo);
        PaymentService payment = new PaymentService();
        BookingService bookingService = new BookingService(repo, payment);

        // create sample data
        Theater t1 = admin.createTheater("T1", "PVR Plaza");
        Movie m1 = admin.createMovie("M1", "Interstellar", 169);

        // seating: rows A-C, cols 1-5; premium: row A
        Map<String, Seat> seats = new HashMap<>();
        for (char r='A'; r<='C'; r++) {
            for (int c=1; c<=5; c++) {
                String id = "" + r + c;
                SeatType type = (r == 'A') ? SeatType.PREMIUM : SeatType.NORMAL;
                double price = (type == SeatType.PREMIUM) ? 300 : 150;
                seats.put(id, new Seat(id, type, price));
            }
        }

        Show show = admin.createShow("S1", "M1", "T1", LocalDateTime.now().plusHours(2), seats);

        // register users
        repo.users.put("U1", new User("U1", "Alice"));
        repo.users.put("U2", new User("U2", "Bob"));

        // Print available shows
        System.out.println("Shows available:");
        repo.shows.values().forEach(s ->
            System.out.println(s.id + " - " + s.movie.title + " @ " + s.theater.name + " - " + s.startTime));

        // Print seating
        show.printSeating();

        // Simulate concurrent bookings for same seat to show correctness
        ExecutorService ex = Executors.newFixedThreadPool(4);
        Callable<Void> task1 = () -> {
            BookingResult r = bookingService.bookSeats("U1", "S1", Arrays.asList("A1", "A2"));
            System.out.println("U1 booking -> " + r.status + " : " + r.message);
            return null;
        };
        Callable<Void> task2 = () -> {
            BookingResult r = bookingService.bookSeats("U2", "S1", Arrays.asList("A1")); // conflicts on A1
            System.out.println("U2 booking -> " + r.status + " : " + r.message);
            return null;
        };

        List<Callable<Void>> tasks = Arrays.asList(task1, task2);
        ex.invokeAll(tasks);
        ex.shutdown();
        ex.awaitTermination(5, TimeUnit.SECONDS);

        // show final seating
        show.printSeating();

        // Display bookings
        System.out.println("Bookings made:");
        repo.bookings.values().forEach(b ->
            System.out.println(b.bookingId + " user:" + b.userId + " seats:" + b.seats + " amt:" + b.amount));
    }
}
