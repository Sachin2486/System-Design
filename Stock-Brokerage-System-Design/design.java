import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/*
 
 Online Stock Brokerage System (LLD)
 
 - User accounts
 - Buy / Sell stocks
 - Portfolio & transaction history
 - Real-time prices (simulated)
 - Strong business validations
*/

enum OrderType {
    BUY, SELL
}

/* ---------------- User ---------------- */
class User {
    final long id;
    final String name;

    User(long id, String name) {
        this.id = id;
        this.name = name;
    }
}

/* ---------------- Stock ---------------- */
class Stock {
    final String symbol;
    volatile double price;
    volatile int availableQuantity;

    Stock(String symbol, double price, int quantity) {
        this.symbol = symbol;
        this.price = price;
        this.availableQuantity = quantity;
    }
}

/* ---------------- Order ---------------- */
class Order {
    final long id;
    final OrderType type;
    final String stockSymbol;
    final int quantity;

    Order(long id, OrderType type, String stockSymbol, int quantity) {
        this.id = id;
        this.type = type;
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
    }
}

/* ---------------- Transaction ---------------- */
class Transaction {
    final long id;
    final OrderType type;
    final String stockSymbol;
    final int quantity;
    final double price;

    Transaction(long id, OrderType type, String stockSymbol, int quantity, double price) {
        this.id = id;
        this.type = type;
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.price = price;
    }

    @Override
    public String toString() {
        return type + " " + quantity + " of " + stockSymbol + " @ " + price;
    }
}

/* ---------------- Trading Account ---------------- */
class TradingAccount {
    final User user;
    double balance;
    final Map<String, Integer> portfolio = new HashMap<>();
    final List<Transaction> transactions = new ArrayList<>();

    TradingAccount(User user, double balance) {
        this.user = user;
        this.balance = balance;
    }
}

/* ---------------- Market Service ---------------- */
class StockMarketService {
    private final Map<String, Stock> stocks = new ConcurrentHashMap<>();

    void addStock(String symbol, double price, int quantity) {
        stocks.put(symbol, new Stock(symbol, price, quantity));
    }

    Stock getStock(String symbol) {
        return stocks.get(symbol);
    }
}

/* ---------------- Order Service ---------------- */
class OrderService {

    private final StockMarketService marketService;
    private final AtomicLong orderIdGen = new AtomicLong(1000);
    private final AtomicLong txnIdGen = new AtomicLong(5000);

    OrderService(StockMarketService marketService) {
        this.marketService = marketService;
    }

    /* ---------------- BUY ---------------- */
    synchronized void buyStock(TradingAccount account, String symbol, int quantity) {
        Stock stock = marketService.getStock(symbol);
        if (stock == null) throw new RuntimeException("Stock not found");

        double totalCost = stock.price * quantity;

        if (account.balance < totalCost)
            throw new RuntimeException("Insufficient balance");

        if (stock.availableQuantity < quantity)
            throw new RuntimeException("Insufficient stock availability");

        // Execute order
        account.balance -= totalCost;
        stock.availableQuantity -= quantity;
        account.portfolio.put(symbol,
                account.portfolio.getOrDefault(symbol, 0) + quantity);

        account.transactions.add(
                new Transaction(txnIdGen.getAndIncrement(),
                        OrderType.BUY, symbol, quantity, stock.price));
    }

    /* ---------------- SELL ---------------- */
    synchronized void sellStock(TradingAccount account, String symbol, int quantity) {
        Integer owned = account.portfolio.get(symbol);
        if (owned == null || owned < quantity)
            throw new RuntimeException("Insufficient stock holdings");

        Stock stock = marketService.getStock(symbol);
        if (stock == null) throw new RuntimeException("Stock not found");

        double totalValue = stock.price * quantity;

        // Execute order
        account.balance += totalValue;
        stock.availableQuantity += quantity;

        if (owned == quantity) account.portfolio.remove(symbol);
        else account.portfolio.put(symbol, owned - quantity);

        account.transactions.add(
                new Transaction(txnIdGen.getAndIncrement(),
                        OrderType.SELL, symbol, quantity, stock.price));
    }
}

/* ---------------- Driver ---------------- */
public class Main {
    public static void main(String[] args) {

        // Setup market
        StockMarketService marketService = new StockMarketService();
        marketService.addStock("AAPL", 150.0, 1000);
        marketService.addStock("GOOG", 2800.0, 500);

        // Create user & account
        User user = new User(1, "Sachin");
        TradingAccount account = new TradingAccount(user, 100_000);

        OrderService orderService = new OrderService(marketService);

        // Buy stocks
        orderService.buyStock(account, "AAPL", 100);
        orderService.buyStock(account, "GOOG", 10);

        // Sell stocks
        orderService.sellStock(account, "AAPL", 40);

        // View portfolio
        System.out.println("Portfolio: " + account.portfolio);
        System.out.println("Balance: " + account.balance);

        // View transactions
        System.out.println("Transactions:");
        account.transactions.forEach(System.out::println);
    }
}
