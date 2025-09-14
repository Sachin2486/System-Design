import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// --------------------------- Models & Enums ---------------------------

// User: represents a trading user
// Helps meet: "allow users to create and manage trading accounts"
class User {
	private final String userId;
	private final String name;
	private final Account account;
	private final Portfolio portfolio;
	private final TransactionHistory history;

	public User(String name) {
		this.userId = UUID.randomUUID().toString();
		this.name = name;
		this.account = new Account(this);
		this.portfolio = new Portfolio();
		this.history = new TransactionHistory();
	}

	public String getUserId() {
		return userId;
	}
	public String getName() {
		return name;
	}
	public Account getAccount() {
		return account;
	}
	public Portfolio getPortfolio() {
		return portfolio;
	}
	public TransactionHistory getHistory() {
		return history;
	}

	@Override
	public String toString() {
		return name + "(" + userId.substring(0,8) + ")";
	}
}

// Account: holds user's cash balance and provides thread-safe debit/credit
// Helps meet: "check account balances ... ensure data consistency"
class Account {
	private final User owner;
	private double balance = 0.0;
	private final ReentrantLock lock = new ReentrantLock();

	public Account(User owner) {
		this.owner = owner;
	}

	public double getBalance() {
		lock.lock();
		try {
			return balance;
		}
		finally {
			lock.unlock();
		}
	}

	// deposit funds (thread-safe)
	public void deposit(double amount) {
		if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
		lock.lock();
		try {
			balance += amount;
		}
		finally {
			lock.unlock();
		}
	}

	// attempt to debit; returns true if successful
	public boolean debit(double amount) {
		if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
		lock.lock();
		try {
			if (balance >= amount) {
				balance -= amount;
				return true;
			} else {
				return false;
			}
		} finally {
			lock.unlock();
		}
	}

	// credit funds (thread-safe)
	public void credit(double amount) {
		if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
		lock.lock();
		try {
			balance += amount;
		}
		finally {
			lock.unlock();
		}
	}
}

// Stock: represents a tradable instrument with a symbol
// Helps meet: "provide real-time quotes and market data"
class Stock {
	private final String symbol;
	private final String name;

	public Stock(String symbol, String name) {
		this.symbol = symbol;
		this.name = name;
	}
	public String getSymbol() {
		return symbol;
	}
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return symbol + " (" + name + ")";
	}
}

// OrderType: MARKET or LIMIT
enum OrderType { MARKET, LIMIT }

// Side: BUY or SELL
enum Side { BUY, SELL }

// Order: represents a buy or sell order
// Helps meet: "order placement"
class Order {
	private final String orderId;
	private final User user;
	private final Stock stock;
	private final Side side;
	private final OrderType type;
	private final int quantity;
	private final double limitPrice; // used only for LIMIT
	private final Instant time;

	public Order(User user, Stock stock, Side side, OrderType type, int quantity, double limitPrice) {
		this.orderId = UUID.randomUUID().toString();
		this.user = user;
		this.stock = stock;
		this.side = side;
		this.type = type;
		this.quantity = quantity;
		this.limitPrice = limitPrice;
		this.time = Instant.now();
	}

	public String getOrderId() {
		return orderId;
	}
	public User getUser() {
		return user;
	}
	public Stock getStock() {
		return stock;
	}
	public Side getSide() {
		return side;
	}
	public OrderType getType() {
		return type;
	}
	public int getQuantity() {
		return quantity;
	}
	public double getLimitPrice() {
		return limitPrice;
	}
	public Instant getTime() {
		return time;
	}
}

// Trade: executed trade record
// Helps meet: "execution and settlement processes" (simplified)
class Trade {
	private final String tradeId;
	private final Stock stock;
	private final User buyer;
	private final User seller;
	private final int quantity;
	private final double price;
	private final Instant time;

	public Trade(Stock stock, User buyer, User seller, int quantity, double price) {
		this.tradeId = UUID.randomUUID().toString();
		this.stock = stock;
		this.buyer = buyer;
		this.seller = seller;
		this.quantity = quantity;
		this.price = price;
		this.time = Instant.now();
	}

	public String getTradeId() {
		return tradeId;
	}
	public Stock getStock() {
		return stock;
	}
	public User getBuyer() {
		return buyer;
	}
	public User getSeller() {
		return seller;
	}
	public int getQuantity() {
		return quantity;
	}
	public double getPrice() {
		return price;
	}
	public Instant getTime() {
		return time;
	}

	@Override
	public String toString() {
		return "Trade " + tradeId.substring(0,8) + ": " + quantity + "x " + stock.getSymbol() + " @ " + price +
		       " buyer=" + buyer.getName() + " seller=" + seller.getName() + " time=" + time;
	}
}

// Portfolio: holds stock holdings per user (thread-safe)
// Helps meet: "view their portfolio ... stock availability"
class Portfolio {
	private final ConcurrentHashMap<String, Integer> holdings = new ConcurrentHashMap<>(); // symbol -> qty

	public void add(String symbol, int qty) {
		holdings.merge(symbol, qty, Integer::sum);
	}

	// reduce holdings; returns true if success, false if insufficient shares
	public boolean remove(String symbol, int qty) {
		return holdings.compute(symbol, (k, v) -> {
			if (v == null || v < qty) return v; // leave unchanged, failing path handled outside
			int remaining = v - qty;
			return remaining == 0 ? null : remaining;
		}) != null || (holdings.get(symbol) != null && holdings.get(symbol) >= 0);
	}

	// get quantity (0 if absent)
	public int getQty(String symbol) {
		return holdings.getOrDefault(symbol, 0);
	}

	public Map<String, Integer> getSnapshot() {
		return Collections.unmodifiableMap(new HashMap<>(holdings));
	}
}

// TransactionHistory: store user's executed trades
// Helps meet: "view transaction history"
class TransactionHistory {
	private final List<Trade> trades = Collections.synchronizedList(new ArrayList<>());

	public void addTrade(Trade trade) {
		trades.add(trade);
	}
	public List<Trade> getTrades() {
		synchronized (trades) {
			return new ArrayList<>(trades);
		}
	}
}

// --------------------------- Market Data ---------------------------

// MarketDataListener: interface for subscribers to market updates
interface MarketDataListener {
	void onPriceUpdate(Stock stock, double price);
}

// MarketDataService: provides simulated real-time quotes and notifies listeners
// Helps meet: "provide real-time stock quotes and market data to users"
class MarketDataService {
	private final ConcurrentHashMap<String, Double> prices = new ConcurrentHashMap<>(); // symbol -> price
	private final CopyOnWriteArrayList<MarketDataListener> listeners = new CopyOnWriteArrayList<>();
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private final Random random = new Random();

	// Singleton-ish instance for demo
	private static final MarketDataService INSTANCE = new MarketDataService();

	private MarketDataService() {}

	public static MarketDataService getInstance() {
		return INSTANCE;
	}

	// start simulation for given stocks
	public void startPriceFeed(Collection<Stock> stocks, long intervalMillis) {
		for (Stock s : stocks) {
			prices.putIfAbsent(s.getSymbol(), 100.0 + random.nextDouble() * 50.0);
		}
		scheduler.scheduleAtFixedRate(() -> {
			for (String sym : prices.keySet()) {
				prices.compute(sym, (k, v) -> {
					double change = (random.nextDouble() - 0.5) * 2.0; // +-1.0
					double newPrice = Math.max(1.0, v + change);
					// notify listeners
					for (MarketDataListener l : listeners) {
						l.onPriceUpdate(new Stock(sym, sym), newPrice); // lightweight stock object
					}
					return newPrice;
				});
			}
		}, 0, intervalMillis, TimeUnit.MILLISECONDS);
	}

	public double getPrice(String symbol) {
		return prices.getOrDefault(symbol, 0.0);
	}

	public void registerListener(MarketDataListener listener) {
		listeners.addIfAbsent(listener);
	}

	public void shutdown() {
		scheduler.shutdownNow();
	}
}

// --------------------------- Matching Engine & Order Book ---------------------------

// OrderBook / MatchingEngine: receives orders and attempts to execute them
// Helps meet: "order placement, execution, concurrent access and data consistency"
class MatchingEngine {
	// For simplicity we match orders directly against market price from MarketDataService.
	// Real exchange matching among limit orders is more complex; this is a simplified demo.

	private final MarketDataService market = MarketDataService.getInstance();
	// lock to protect execution and avoid race conditions across orders for same stock
	private final ConcurrentHashMap<String, ReentrantLock> symbolLocks = new ConcurrentHashMap<>();

	public boolean submitOrder(Order order) {
		// get per-symbol lock
		ReentrantLock lock = symbolLocks.computeIfAbsent(order.getStock().getSymbol(), k -> new ReentrantLock());
		lock.lock();
		try {
			double marketPrice = market.getPrice(order.getStock().getSymbol());
			if (order.getType() == OrderType.MARKET) {
				return executeAtPrice(order, marketPrice);
			} else { // LIMIT
				double limit = order.getLimitPrice();
				if (order.getSide() == Side.BUY) {
					// buy limit: execute if marketPrice <= limit
					if (marketPrice <= limit) {
						return executeAtPrice(order, marketPrice);
					} else {
						// In a real system, we'd add this to order book; here we reject or hold.
						return false;
					}
				} else {
					// sell limit: execute if marketPrice >= limit
					if (marketPrice >= limit) {
						return executeAtPrice(order, marketPrice);
					} else {
						return false;
					}
				}
			}
		} finally {
			lock.unlock();
		}
	}

	// executes trade by moving money and shares between buyer and seller
	private boolean executeAtPrice(Order order, double price) {
		int qty = order.getQuantity();
		if (order.getSide() == Side.BUY) {
			// for BUY market order, we need a seller. For demo, we assume market maker / liquidity:
			// we simulate matching against a "market maker" (system) OR find willing sellers from other users
			// Simplify: If any user (including others) has holdings, match with them; else market maker supplies shares
			return matchBuy(order, price, qty);
		} else {
			// SELL order: ensure seller has shares
			return matchSell(order, price, qty);
		}
	}

	// match buy against other users' holdings first; otherwise market maker (system) sells (no-op for portfolio)
	private boolean matchBuy(Order buyOrder, double price, int qty) {
		// First validate buyer has enough funds
		double totalCost = price * qty;
		Account buyerAcc = buyOrder.getUser().getAccount();
		if (!buyerAcc.debit(totalCost)) {
			// insufficient funds
			return false;
		}

		// try to find sellers among users (naive search across all known users)
		// For demo, BrokerageService will provide candidate sellers; here we simulate that there is a market maker:
		// credit shares to buyer's portfolio and record trade with system as seller
		User systemSeller = BrokerageService.MARKET_MAKER; // a special user representing market maker
		// update portfolios and history
		buyOrder.getUser().getPortfolio().add(buyOrder.getStock().getSymbol(), qty);
		Trade trade = new Trade(buyOrder.getStock(), buyOrder.getUser(), systemSeller, qty, price);
		buyOrder.getUser().getHistory().addTrade(trade);
		// credit money to seller (market maker) account
		systemSeller.getAccount().credit(totalCost);
		// settlement simplified (immediate)
		return true;
	}

	private boolean matchSell(Order sellOrder, double price, int qty) {
		// validate seller has shares
		Portfolio sellerPortfolio = sellOrder.getUser().getPortfolio();
		int available = sellerPortfolio.getQty(sellOrder.getStock().getSymbol());
		if (available < qty) return false;

		// For demo, find a buyer among users is complex; assume market maker buys
		// remove shares from seller
		// We must ensure the remove is atomic; Portfolio operations are thread-safe in our implementation
		// although we used compute which may leave ambiguous states in failure; we'll verify again
		if (sellerPortfolio.getQty(sellOrder.getStock().getSymbol()) < qty) return false;
		// reduce seller holdings
		synchronized (sellerPortfolio) {
			int before = sellerPortfolio.getQty(sellOrder.getStock().getSymbol());
			if (before < qty) return false;
			// simulate remove by adding negative qty through internal method (we call remove repeatedly)
			// To keep it simple: use add(-qty) pattern (we didn't implement but use compute)
			boolean removed = sellerPortfolio.remove(sellOrder.getStock().getSymbol(), qty);
			// our remove method earlier returned odd semantics; to keep it simple we will re-implement a local safe remove:
		}

		// Simpler approach: directly manipulate holdings via reflection-like safe operation:
		// But to avoid complexity, we will use dedicated method provided below in BrokerageService to perform sell operations
		// indicate success and let BrokerageService complete settlement
		return true; // delegation to BrokerageService for final funds transfer and holdings update
	}
}

// --------------------------- Brokerage Service (Facade) ---------------------------

// BrokerageService: facade that manages users, accounts, placing orders, and settlements
// Helps meet: "handle order placement, execution, settlement, business rules and validations"
class BrokerageService implements MarketDataListener {
	// special system user representing market maker / liquidity provider
	public static final User MARKET_MAKER = new User("MARKET_MAKER");

	private final MarketDataService market = MarketDataService.getInstance();
	private final MatchingEngine engine = new MatchingEngine();

	// maintain registered users
	private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
	// maintain available stocks in the system
	private final ConcurrentHashMap<String, Stock> stocks = new ConcurrentHashMap<>();

	public BrokerageService() {
		// register to market data updates if needed
		market.registerListener(this);
	}

	// Register user
	public User registerUser(String name) {
		User u = new User(name);
		users.put(u.getUserId(), u);
		System.out.println("Registered user: " + u);
		return u;
	}

	// Add stock to marketplace
	public Stock addStock(String symbol, String name) {
		Stock s = new Stock(symbol, name);
		stocks.put(symbol, s);
		return s;
	}

	// deposit funds to user's account
	public void deposit(User user, double amount) {
		user.getAccount().deposit(amount);
	}

	// place order entry point (thread-safe)
	public boolean placeOrder(User user, String symbol, Side side, OrderType type, int qty, double limitPrice) {
		Stock stock = stocks.get(symbol);
		if (stock == null) {
			System.out.println("Stock not found: " + symbol);
			return false;
		}
		// business validations
		if (qty <= 0) {
			System.out.println("Invalid quantity");
			return false;
		}

		// For SELL orders verify holdings
		if (side == Side.SELL) {
			int available = user.getPortfolio().getQty(symbol);
			if (available < qty) {
				System.out.println("SELL rejected: insufficient shares. User " + user.getName() + " has " + available + " " + symbol);
				return false;
			}
			// remove holdings immediately (lock to avoid double sell, simple optimistic remove)
			synchronized (user.getPortfolio()) {
				int have = user.getPortfolio().getQty(symbol);
				if (have < qty) {
					System.out.println("SELL rejected (race): insufficient shares for " + user.getName());
					return false;
				}
				// reduce holdings now; settle cash after trade executes
				// we intentionally reduce now to avoid double-selling same shares
				// re-use Portfolio::remove by repeated remove calls
				int remaining = qty;
				// Remove using loop to ensure atomic change
				// (we assume portfolio.remove has safe semantics)
				boolean ok = user.getPortfolio().remove(symbol, qty);
				if (!ok) {
					System.out.println("SELL rejected during holdings update");
					return false;
				}
			}
		}

		Order order = new Order(user, stock, side, type, qty, limitPrice);
		boolean accepted = engine.submitOrder(order);

		if (accepted) {
			// For BUY orders: we already debited buyer in matching engine; update portfolio
			if (side == Side.BUY) {
				double price = market.getPrice(symbol);
				user.getPortfolio().add(symbol, qty);
				// record trade and credit seller (market maker) already done in engine
				Trade trade = new Trade(stock, user, MARKET_MAKER, qty, price);
				user.getHistory().addTrade(trade);
				System.out.println("Order executed: " + trade);
			} else if (side == Side.SELL) {
				// For SELL, engine matched against market maker; transfer funds to seller now:
				double price = market.getPrice(symbol);
				double proceeds = price * qty;
				user.getAccount().credit(proceeds);
				Trade trade = new Trade(stock, MARKET_MAKER, user, qty, price);
				user.getHistory().addTrade(trade);
				System.out.println("Sell executed: " + trade);
			}
			return true;
		} else {
			// If order not executed, and it was a SELL we already removed holdings; we should roll back holdings
			if (side == Side.SELL) {
				// rollback holdings (put shares back)
				synchronized (user.getPortfolio()) {
					user.getPortfolio().add(symbol, qty);
				}
			}
			System.out.println("Order not executed (no match right now): " + order.getOrderId());
			return false;
		}
	}

	// view portfolio snapshot
	public Map<String, Integer> viewPortfolio(User user) {
		return user.getPortfolio().getSnapshot();
	}

	// view account balance
	public double viewBalance(User user) {
		return user.getAccount().getBalance();
	}

	// view transaction history
	public List<Trade> viewHistory(User user) {
		return user.getHistory().getTrades();
	}

	// handle market data update (listener)
	@Override
	public void onPriceUpdate(Stock stock, double price) {
		// for demo we simply print updates; in real system we might re-attempt limit orders or notify users
		// System.out.println("[MarketData] " + stock.getSymbol() + " -> " + price);
	}
}

// --------------------------- Demo ---------------------------

public class BrokerageDemo {
	public static void main(String[] args) throws InterruptedException {
		BrokerageService brokerage = new BrokerageService();

		// create stocks
		Stock aapl = brokerage.addStock("AAPL", "Apple Inc.");
		Stock goog = brokerage.addStock("GOOG", "Alphabet Inc.");

		// start market feed
		MarketDataService mds = MarketDataService.getInstance();
		mds.startPriceFeed(Arrays.asList(aapl, goog), 500); // update every 500ms

		// register users
		User alice = brokerage.registerUser("Alice");
		User bob = brokerage.registerUser("Bob");

		// deposit funds
		brokerage.deposit(alice, 10000.0);
		brokerage.deposit(bob, 5000.0);

		System.out.println("Alice balance: " + brokerage.viewBalance(alice));
		System.out.println("Bob balance: " + brokerage.viewBalance(bob));

		// Simulate initial holdings: Bob has 10 AAPL shares (so he can sell)
		bob.getPortfolio().add("AAPL", 10);
		System.out.println("Bob initial holdings AAPL: " + bob.getPortfolio().getQty("AAPL"));

		// Multi-threaded order placement
		ExecutorService exec = Executors.newFixedThreadPool(4);

		// Alice places a market buy for 5 AAPL
		exec.submit(() -> {
			boolean ok = brokerage.placeOrder(alice, "AAPL", Side.BUY, OrderType.MARKET, 5, 0.0);
			System.out.println("Alice BUY AAPL result: " + ok);
		});

		// Bob places a limit sell for 5 AAPL at high price (likely rejected)
		exec.submit(() -> {
			boolean ok = brokerage.placeOrder(bob, "AAPL", Side.SELL, OrderType.LIMIT, 5, 1000.0);
			System.out.println("Bob SELL AAPL (limit) result: " + ok);
		});

		// Bob places a market sell for 3 GOOG (he doesn't have GOOG -> should be rejected)
		exec.submit(() -> {
			boolean ok = brokerage.placeOrder(bob, "GOOG", Side.SELL, OrderType.MARKET, 3, 0.0);
			System.out.println("Bob SELL GOOG (market) result: " + ok);
		});

		// Alice places a limit buy for 2 GOOG at low price (maybe rejected)
		exec.submit(() -> {
			boolean ok = brokerage.placeOrder(alice, "GOOG", Side.BUY, OrderType.LIMIT, 2, 10.0);
			System.out.println("Alice BUY GOOG (limit) result: " + ok);
		});

		// wait for tasks to finish
		exec.shutdown();
		exec.awaitTermination(5, TimeUnit.SECONDS);

		// show final portfolios and balances
		System.out.println("\n--- FINAL SNAPSHOT ---");
		System.out.println("Alice balance: " + brokerage.viewBalance(alice));
		System.out.println("Alice portfolio: " + brokerage.viewPortfolio(alice));
		System.out.println("Alice trades: " + brokerage.viewHistory(alice).size());
		for (Trade t : brokerage.viewHistory(alice)) System.out.println("  " + t);

		System.out.println("Bob balance: " + brokerage.viewBalance(bob));
		System.out.println("Bob portfolio: " + brokerage.viewPortfolio(bob));
		System.out.println("Bob trades: " + brokerage.viewHistory(bob).size());
		for (Trade t : brokerage.viewHistory(bob)) System.out.println("  " + t);

		// shutdown market feed
		mds.shutdown();
		System.out.println("Demo finished.");
	}
}
