import java.time.LocalDateTime;
import java.util.*;

// --- Models ---
class Room {
	private String name;
	private int capacity;

	public  Room(String name, int capacity) {
		this.name = name;
		this.capacity = capacity;
	}

	public String getName() {
		return name;
	}

	public int getCapacity() {
		return capacity;
	}

	@Override
	public String toString() {
		return name + " (" + capacity + " seats)";
	}
}

class User {
	private String name;

	public User (String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}
}

class Booking {
	private User user;
	private Room room;
	private LocalDateTime start;
	private LocalDateTime end;

	public Booking(User user, Room room, LocalDateTime start, LocalDateTime end) {
		this.user = user;
		this.room = room;
		this.start = start;
		this.end = end;
	}

	public User getUser() {
		return user;
	}
	public Room getRoom() {
		return room;
	}
	public LocalDateTime getStart() {
		return start;
	}
	public LocalDateTime getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return "Booking by " + user.getName() + " in " + room.getName() +
		       " from " + start + " to " + end;
	}
}

// --- Singleton Repository ---
class BookingRepository {
    private static BookingRepository instance;
    private List<Room> rooms = new ArrayList<>();
    private List<Booking> bookings = new ArrayList<>();

    private BookingRepository() {}

    public static synchronized BookingRepository getInstance() {
        if (instance == null) instance = new BookingRepository();
        return instance;
    }

    public void addRoom(Room room) { rooms.add(room); }
    public List<Room> getRooms() { return rooms; }

    public void addBooking(Booking booking) { bookings.add(booking); }
    public List<Booking> getBookings() { return bookings; }
}

// --- Services ---
class BookingService {
    private BookingRepository repo = BookingRepository.getInstance();

    public boolean isAvailable(Room room, LocalDateTime start, LocalDateTime end) {
        for (Booking b : repo.getBookings()) {
            if (b.getRoom().equals(room)) {
                boolean overlap = start.isBefore(b.getEnd()) && end.isAfter(b.getStart());
                if (overlap) return false;
            }
        }
        return true;
    }

    public boolean createBooking(User user, Room room, LocalDateTime start, LocalDateTime end) {
        if (isAvailable(room, start, end)) {
            repo.addBooking(new Booking(user, room, start, end));
            return true;
        }
        return false;
    }

    public void showBookings() {
        if (repo.getBookings().isEmpty()) {
            System.out.println("No bookings yet.");
        } else {
            for (Booking b : repo.getBookings()) System.out.println(b);
        }
    }
}

// --- Controller (main flow) ---
class MeetingRoomApp {
    private Scanner scanner = new Scanner(System.in);
    private BookingService bookingService = new BookingService();
    private BookingRepository repo = BookingRepository.getInstance();

    public void start() {
        System.out.println("Welcome to Meeting Room Booking System");

        // Setup sample rooms
        repo.addRoom(new Room("Orchid", 8));
        repo.addRoom(new Room("Lotus", 10));

        while (true) {
            System.out.println("\n1. View Rooms\n2. Create Booking\n3. Show All Bookings\n4. Exit");
            System.out.print("Enter choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // clear buffer

            switch (choice) {
                case 1 -> showRooms();
                case 2 -> createBooking();
                case 3 -> bookingService.showBookings();
                case 4 -> {
                    System.out.println("Exiting...");
                    return;
                }
                default -> System.out.println("Invalid choice!");
            }
        }
    }

    private void showRooms() {
        System.out.println("\nAvailable Rooms:");
        for (Room room : repo.getRooms()) System.out.println("- " + room);
    }

    private void createBooking() {
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        User user = new User(name);

        System.out.println("\nSelect room number:");
        List<Room> rooms = repo.getRooms();
        for (int i = 0; i < rooms.size(); i++) {
            System.out.println((i + 1) + ". " + rooms.get(i));
        }
        int roomChoice = scanner.nextInt() - 1;
        Room room = rooms.get(roomChoice);

        System.out.print("Enter start hour (24h format): ");
        int startHour = scanner.nextInt();
        System.out.print("Enter end hour: ");
        int endHour = scanner.nextInt();

        LocalDateTime start = LocalDateTime.now().withHour(startHour).withMinute(0);
        LocalDateTime end = LocalDateTime.now().withHour(endHour).withMinute(0);

        if (bookingService.createBooking(user, room, start, end)) {
            System.out.println("Booking confirmed!");
        } else {
            System.out.println("Room not available at that time!");
        }
    }
}

// --- Entry Point ---
public class MeetingRoomSystem {
    public static void main(String[] args) {
        new MeetingRoomApp().start();
    }
}