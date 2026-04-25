import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

enum SeatType {
	PREMIUM,
	NORMAL
}

enum SeatStatus {
	AVAILABLE,
	BOOKED
}

class Movie {
	String id;
	String name;

	Movie(String id, String name) {
		this.id = id;
		this.name = name;
	}
}

class Seat {
	String seatId;
	SeatType type;
	double price;
	SeatStatus status;

	Seat(String id, SeatType type, double price) {
		this.seatId = id;
		this.type = type;
		this.price = price;
		this.status = SeatStatus.AVAILABLE;
	}
}

/* ================= SHOW ================= */
class Show {
	String showId;
	Movie movie;
	Map<String, Seat> seats = new HashMap<>();
	ReentrantLock lock = new ReentrantLock();

	Show(String id, Movie movie) {
		this.showId = id;
		this.movie = movie;
	}

	void addSeat(Seat seat) {
		seats.put(seat.seatId, seat);
	}
}

class Booking {
	String bookingId;
	List<Seat> seats;
	double amount;

	Booking(String id, List<Seat> seats, double amount) {
		this.bookingId = id;
		this.seats = seats;
		this.amount = amount;
	}
}

interface PaymentStrategy {
	void pay(double amount);
}

class CardPayment implements PaymentStrategy {
	public void pay(double amount) {
		System.out.println("Paid " + amount + "via Card");
	}
}

class UpiPayment implements PaymentStrategy {
	public void pay(double amount) {
		System.out.println("Paid " + amount + "via UPI");
	}
}

/* ================= BOOKING SERVICE ================= */
class BookingService {

	Map<String, Show> shows = new HashMap<>();

	Booking bookTickets(String showId, List<String> seatIds, PaymentStrategy payment) {
		Show show = shows.get(showId);

		show.lock.lock(); // concurrency control
		try {
			List<Seat> selectedSeats = new ArrayList<>();
			double total = 0;

			for (String seatId : seatIds) {
				Seat seat = show.seats.get(seatId);

				if (seat.status == SeatStatus.BOOKED) {
					throw new RuntimeException("Seat already booked: " + seatId);
				}

				selectedSeats.add(seat);
				total += seat.price;
			}

			// mark booked
			for (Seat seat : selectedSeats) {
				seat.status = SeatStatus.BOOKED;
			}

			// payment
			payment.pay(total);

			return new Booking(UUID.randomUUID().toString(), selectedSeats, total);

		} finally {
			show.lock.unlock();
		}
	}
}

public class Main
{
	public static void main(String[] args) {

		Movie movie = new Movie("M1", "Inception");

		Show show = new Show("S1", movie);

		// Add seats
		show.addSeat(new Seat("A1", SeatType.NORMAL, 100));
		show.addSeat(new Seat("A2", SeatType.NORMAL, 100));
		show.addSeat(new Seat("P1", SeatType.PREMIUM, 200));

		BookingService service = new BookingService();
		service.shows.put("S1", show);

		Booking booking = service.bookTickets(
		                      "S1",
		                      List.of("A1", "P1"),
		                      new CardPayment()
		                  );

		System.out.println("Booking successful. Amount: " + booking.amount);
	}
}



