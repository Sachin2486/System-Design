import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/* Represents a hotel room with type, price, and availability */
class Room {
	private int roomId;
	private String type;
	private double pricePerNight;
	private boolean available;

	public Room(int roomId, String type,double pricePerNight) {
		this.roomId = roomId;
		this.type = type;
		this.pricePerNight = pricePerNight;
		this.available = available;
	}

	public int getRoomId() {
		return roomId;
	}

	public String getType() {
		return type;
	}

	public double getPricePerNight() {
		return pricePerNight;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}
}

/* Represents a guest with personal and payment info */
class Guest {
	private String name;
	private String contact;
	private String idProof;

	public Guest(String name, String contact, String idProof) {
		this.name = name;
		this.contact = contact;
		this.idProof = idProof;
	}

	public String getName() {
		return name;
	}
}

/* Represents a payment made for a reservation */
class Payment {
	private double amount;
	private String method; // cash, credit, online
	private Date timestamp;

	public Payment(double amount, String method, Date timestamp) {
		this.amount = amount;
		this.method = method;
		this.timestamp = new Date();
	}

	public Payment(double amount, String method) {
		this(amount, method, new Date()); // default to current date
	}

	public String getDetails() {
		return "Paid " + amount + " via " + method + " on " + timestamp;
	}

}

/* Represents a reservation between guest and room */
class Reservation {
	private String reservationId;
	private Guest guest;
	private Room room;
	private Date checkInDate;
	private Date checkOutDate;
	private String status; // booked, checked-in, checked-out
	private Payment payment;

	public Reservation(String reservationId, Guest guest, Room room, Date checkIn, Date checkOut) {
		this.reservationId = reservationId;
		this.guest = guest;
		this.room = room;
		this.checkInDate = checkIn;
		this.checkOutDate = checkOut;
		this.status = "Booked";
	}

	public String getReservationId() {
		return reservationId;
	}
	public Guest getGuest() {
		return guest;
	}
	public Room getRoom() {
		return room;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public void setPayment(Payment payment) {
		this.payment = payment;
	}
	public Payment getPayment() {
		return payment;
	}
}

/* Singleton class to manage hotel data (rooms, guests, reservations) */
class HotelManager {
	private static HotelManager instance;
	private Map<Integer, Room> rooms = new ConcurrentHashMap<>();
	private Map<String, Reservation> reservations = new ConcurrentHashMap<>();

	private HotelManager() {}

	public static synchronized HotelManager getInstance() {
		if (instance == null) instance = new HotelManager();
		return instance;
	}

	public void addRoom(Room room) {
		rooms.put(room.getRoomId(), room);
	}

	public Room getAvailableRoomByType(String type) {
		for (Room r : rooms.values()) {
			if (r.getType().equalsIgnoreCase(type) && r.isAvailable()) {
				return r;
			}
		}
		return null;
	}

	public void addReservation(Reservation res) {
		reservations.put(res.getReservationId(), res);
	}

	public Reservation getReservation(String id) {
		return reservations.get(id);
	}

	public Collection<Reservation> getAllReservations() {
		return reservations.values();
	}
}

/* Service layer to handle booking, check-in, check-out logic */
class BookingService {

	// Book a room by type and create reservation
	public synchronized Reservation bookRoom(Guest guest, String roomType, Date in, Date out) {
		Room room = HotelManager.getInstance().getAvailableRoomByType(roomType);
		if (room == null) {
			System.out.println("No " + roomType + " rooms available.");
			return null;
		}
		room.setAvailable(false);
		String resId = UUID.randomUUID().toString();
		Reservation res = new Reservation(resId, guest, room, in, out);
		HotelManager.getInstance().addReservation(res);
		System.out.println("Room " + room.getRoomId() + " booked successfully for " + guest.getName());
		return res;
	}

	// Check-in the guest
	public synchronized void checkIn(String reservationId) {
		Reservation res = HotelManager.getInstance().getReservation(reservationId);
		if (res != null && res.getStatus().equals("Booked")) {
			res.setStatus("Checked-In");
			System.out.println("Guest " + res.getGuest().getName() + " checked in to Room " + res.getRoom().getRoomId());
		}
	}

	// Check-out the guest and free the room
	public synchronized void checkOut(String reservationId, String paymentMethod) {
		Reservation res = HotelManager.getInstance().getReservation(reservationId);
		if (res != null && res.getStatus().equals("Checked-In")) {
			res.setStatus("Checked-Out");
			res.getRoom().setAvailable(true);
			double totalBill = res.getRoom().getPricePerNight(); // simple bill logic
			Payment payment = new Payment(totalBill, paymentMethod);
			res.setPayment(payment);
			System.out.println("Guest " + res.getGuest().getName() + " checked out. " + payment.getDetails());
		}
	}
}

/* Demo class to simulate the system */
public class HotelSystemDemo {
	public static void main(String[] args) {
		// Setup rooms
		HotelManager manager = HotelManager.getInstance();
		manager.addRoom(new Room(101, "Single", 1500));
		manager.addRoom(new Room(102, "Double", 2500));
		manager.addRoom(new Room(201, "Suite", 5000));

		// Create guest
		Guest guest = new Guest("Rahul", "9876543210", "Aadhar123");

		// Book room
		BookingService service = new BookingService();
		Reservation res = service.bookRoom(guest, "Double", new Date(), new Date());

		if (res != null) {
			// Check in and out
			service.checkIn(res.getReservationId());
			service.checkOut(res.getReservationId(), "Credit Card");
		}
	}
}





