import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.time.*;

// User model: represents a registered user who can create auctions and bid
class User {
	private final String userId;
	private final String name;
	private final String email;

	public User(String userId, String name, String email) {
		this.userId = userId;
		this.name = name;
		this.email = email;
	}
	
	public User(String name, String email) {
        this.userId = UUID.randomUUID().toString();
        this.name = name;
        this.email = email;
    }
    
	public String getUserId() {
		return userId;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public String toString() {
		return name + "(" + userId.substring(0,8) + ")";
	}
}

// Bid model: represents a bid placed by a user at a particular time
class Bid {
	private final User bidder;
	private final double amount;
	private final Instant time;

	public Bid(User bidder, double amount) {
		this.bidder = bidder;
		this.amount = amount;
		this.time = Instant.now();
	}
	
	public User getBidder() {
		return bidder;
	}

	public double getAmount() {
		return amount;
	}

	public Instant getTime() {
		return time;
	}
}

// AuctionItem: details of the item being auctioned
class AuctionItem {
	private final String itemId;
	private final String title;
	private final String description;
	private final String category;

	public  AuctionItem(String title, String description, String category) {
		this.itemId = UUID.randomUUID().toString();
		this.title = title;
		this.description = description;
		this.category = category;
	}

	public String getItemId() {
		return itemId;
	}
	public String getTitle() {
		return title;
	}
	public String getDescription() {
		return description;
	}
	public String getCategory() {
		return category;
	}

	@Override
	public String toString() {
		return title + " [" + category + "] - " + description + " (id:" + itemId.substring(0,8) + ")";
	}
}

// Notifier: abstraction to notify users (console, email, push, etc.)
interface Notifier {
    void notify(User user, String message);
}

// ConsoleNotifier: simple notifier implementation that prints messages to console
class ConsoleNotifier implements Notifier {
    public void notify(User user, String message) {
        System.out.println("[NOTIFY] To " + user.getName() + ": " + message);
    }
}

// Auction: represents a single auction, handles concurrent bids and maintains state
class Auction {
    enum State { ACTIVE, ENDED }

    private final String auctionId;
    private final AuctionItem item;
    private final User seller;
    private final double startingPrice;
    private volatile double currentHighest;        // updated under lock
    private volatile User currentHighestBidder;    // updated under lock
    private final List<Bid> bidHistory = new CopyOnWriteArrayList<>();
    private final Instant startTime;
    private final Instant endTime;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile State state = State.ACTIVE;
    private final Notifier notifier;

    public Auction(AuctionItem item, User seller, double startingPrice, Duration duration, Notifier notifier) {
        this.auctionId = UUID.randomUUID().toString();
        this.item = item;
        this.seller = seller;
        this.startingPrice = startingPrice;
        this.currentHighest = startingPrice;
        this.currentHighestBidder = null;
        this.startTime = Instant.now();
        this.endTime = startTime.plus(duration);
        this.notifier = notifier;
    }

    public String getAuctionId() { return auctionId; }
    public AuctionItem getItem() { return item; }
    public User getSeller() { return seller; }
    public double getStartingPrice() { return startingPrice; }
    public Instant getEndTime() { return endTime; }
    public State getState() { return state; }

    // Place bid: thread-safe, returns true if accepted
    public boolean placeBid(User bidder, double amount) {
        if (state != State.ACTIVE) {
            return false;
        }
        if (Instant.now().isAfter(endTime)) {
            // auction should be ended by scheduler, but double-check here
            endAuction();
            return false;
        }
        lock.lock();
        try {
            // only accept strictly higher bids
            double minAccept = (currentHighestBidder == null) ? startingPrice : currentHighest + 0.01;
            if (amount <= currentHighest) {
                return false;
            }
            // record bid
            Bid bid = new Bid(bidder, amount);
            bidHistory.add(bid);
            User previousHighest = currentHighestBidder;
            currentHighest = amount;
            currentHighestBidder = bidder;

            // notify previous highest bidder that they've been outbid
            if (previousHighest != null && !previousHighest.getUserId().equals(bidder.getUserId())) {
                notifier.notify(previousHighest, "You have been outbid on item '" + item.getTitle() +
                        "'. New highest: " + amount + " by " + bidder.getName());
            }
            // notify seller about new bid
            notifier.notify(seller, "New bid of " + amount + " by " + bidder.getName() + " on your listing '" + item.getTitle() + "'.");

            return true;
        } finally {
            lock.unlock();
        }
    }

    // End auction, determine winner and notify
    public void endAuction() {
        // avoid double end
        if (state == State.ENDED) return;
        lock.lock();
        try {
            if (state == State.ENDED) return;
            state = State.ENDED;
            if (currentHighestBidder != null) {
                notifier.notify(currentHighestBidder, "Congratulations! You won the auction for '" + item.getTitle() +
                        "' with bid " + currentHighest);
                notifier.notify(seller, "Your item '" + item.getTitle() + "' sold to " + currentHighestBidder.getName() +
                        " for " + currentHighest);
            } else {
                notifier.notify(seller, "Your item '" + item.getTitle() + "' did not receive any bids and the auction ended.");
            }
        } finally {
            lock.unlock();
        }
    }

    public List<Bid> getBidHistory() { return Collections.unmodifiableList(bidHistory); }
    public double getCurrentHighest() { return currentHighest; }
    public User getCurrentHighestBidder() { return currentHighestBidder; }

    @Override
    public String toString() {
        return "Auction[" + auctionId.substring(0,8) + "] " + item.getTitle() +
                " | seller=" + seller.getName() +
                " | currentHighest=" + currentHighest +
                " | ends=" + endTime;
    }
}

// AuctionHouse: facade/service managing users, auctions, searches, and scheduling auction ends
class AuctionHouse {
    private final Map<String, User> users = new ConcurrentHashMap<>();                 // userId->User
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();           // auctionId->Auction
    private final Notifier notifier;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public AuctionHouse(Notifier notifier) {
        this.notifier = notifier;
    }

    // Register a user 
   public User registerUser(String name, String email) {
        User u = new User(name, email);
        users.put(u.getUserId(), u);
        System.out.println("Registered user: " + u);
        return u;
    }

    // Create auction listing and schedule its end
    public Auction createAuction(User seller, String title, String desc, String category, double startingPrice, Duration duration) {
        AuctionItem item = new AuctionItem(title, desc, category);
        Auction auction = new Auction(item, seller, startingPrice, duration, notifier);
        auctions.put(auction.getAuctionId(), auction);

        long delay = Math.max(0, Duration.between(Instant.now(), auction.getEndTime()).toMillis());
        scheduler.schedule(() -> {
            auction.endAuction();
            auctions.remove(auction.getAuctionId()); // optional: remove ended auctions from active map
        }, delay, TimeUnit.MILLISECONDS);

        System.out.println("Created auction: " + auction);
        return auction;
    }

    // Place a bid on an auction
    public boolean placeBid(User bidder, String auctionId, double amount) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) {
            System.out.println("Auction not found or already ended: " + auctionId);
            return false;
        }
        boolean accepted = auction.placeBid(bidder, amount);
        if (accepted) {
            System.out.println("Bid accepted: " + bidder.getName() + " -> " + amount + " on " + auction.getItem().getTitle());
        } else {
            System.out.println("Bid rejected: " + bidder.getName() + " -> " + amount + " on " + auction.getItem().getTitle());
        }
        return accepted;
    }

    // Browse active auctions
    public List<Auction> listActiveAuctions() {
        List<Auction> list = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getState() == Auction.State.ACTIVE) list.add(a);
        }
        return list;
    }

    // Search by title / category / price range
    public List<Auction> search(String titleContains, String category, Double minPrice, Double maxPrice) {
        List<Auction> res = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getState() != Auction.State.ACTIVE) continue;
            boolean ok = true;
            if (titleContains != null && !a.getItem().getTitle().toLowerCase().contains(titleContains.toLowerCase())) ok = false;
            if (category != null && !a.getItem().getCategory().equalsIgnoreCase(category)) ok = false;
            double cur = a.getCurrentHighest();
            if (minPrice != null && cur < minPrice) ok = false;
            if (maxPrice != null && cur > maxPrice) ok = false;
            if (ok) res.add(a);
        }
        return res;
    }

    // Manually end all auctions and shutdown (for demo cleanup)
    public void shutdown() {
        scheduler.shutdownNow();
    }
}

// Demo of the AuctionSystem
public class AuctionSystemDemo {
    public static void main(String[] args) throws InterruptedException {
        Notifier notifier = new ConsoleNotifier();
        AuctionHouse house = new AuctionHouse(notifier);

        // Register users
        User alice = house.registerUser("Alice", "alice@example.com");
        User bob   = house.registerUser("Bob", "bob@example.com");
        User carol = house.registerUser("Carol", "carol@example.com");

        // Alice creates an auction that lasts 6 seconds
        Auction a1 = house.createAuction(alice, "Vintage Watch", "Classic mechanical watch", "Accessories", 100.0, Duration.ofSeconds(6));

        // Bob and Carol browse active auctions
        System.out.println("\nActive auctions:");
        for (Auction a : house.listActiveAuctions()) System.out.println("  " + a);

        // Simulate concurrent bidding using threads
        ExecutorService exec = Executors.newFixedThreadPool(4);

        exec.submit(() -> house.placeBid(bob, a1.getAuctionId(), 110.0));
        exec.submit(() -> {
            sleepMillis(500);
            house.placeBid(carol, a1.getAuctionId(), 120.0);
        });
        exec.submit(() -> {
            sleepMillis(800);
            house.placeBid(bob, a1.getAuctionId(), 130.0);
        });
        exec.submit(() -> {
            sleepMillis(1000);
            house.placeBid(carol, a1.getAuctionId(), 125.0); // lower than current highest, should be rejected
        });

        // Wait for auction to end (scheduler will call endAuction)
        Thread.sleep(8000); // wait slightly longer than auction duration

        System.out.println("\nAfter auction end, attempting to bid (should fail):");
        house.placeBid(bob, a1.getAuctionId(), 200.0);

        exec.shutdownNow();
        house.shutdown();
        System.out.println("\nDemo finished.");
    }

    private static void sleepMillis(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
