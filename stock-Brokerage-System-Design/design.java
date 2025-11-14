import java.util.*;

enum OrderType { BUY, SELL }

class Order {
    String userId;
    String stock;
    int quantity;
    double price;
    OrderType type;

    public Order(String userId, String stock, int quantity, double price, OrderType type) {
        this.userId = userId;
        this.stock = stock;
        this.quantity = quantity;
        this.price = price;
        this.type = type;
    }
}

class Portfolio {
    Map<String, Integer> holdings = new HashMap<>();
    double balance = 100000;

    void addStock(String stock, int qty) {
        holdings.put(stock, holdings.getOrDefault(stock, 0) + qty);
    }

    void removeStock(String stock, int qty) {
        holdings.put(stock, holdings.getOrDefault(stock, 0) - qty);
        if (holdings.get(stock) <= 0) holdings.remove(stock);
    }
}

class User {
    String id;
    Portfolio portfolio = new Portfolio();

    public User(String id) {
        this.id = id;
    }
}

class OrderBook {
    List<Order> buyOrders = new ArrayList<>();
    List<Order> sellOrders = new ArrayList<>();

    public void addOrder(Order order) {
        if (order.type == OrderType.BUY) {
            buyOrders.add(order);
            buyOrders.sort(Comparator.comparingDouble((Order o) -> o.price).reversed());
        } else {
            sellOrders.add(order);
            sellOrders.sort(Comparator.comparingDouble((Order o) -> o.price));
        }
    }
}

class Market {
    Map<String, OrderBook> books = new HashMap<>();

    public OrderBook getBook(String stock) {
        books.putIfAbsent(stock, new OrderBook());
        return books.get(stock);
    }
}

class BrokerageService {
    Map<String, User> users = new HashMap<>();
    Market market = new Market();

    public User createUser(String id) {
        User u = new User(id);
        users.put(id, u);
        return u;
    }

    public void placeOrder(Order order) {
        User user = users.get(order.userId);
        OrderBook book = market.getBook(order.stock);

        if (order.type == OrderType.BUY && user.portfolio.balance < order.price * order.quantity) {
            System.out.println("Insufficient balance.");
            return;
        }

        if (order.type == OrderType.SELL &&
                user.portfolio.holdings.getOrDefault(order.stock, 0) < order.quantity) {
            System.out.println("Insufficient stock.");
            return;
        }

        matchOrder(order, book);
    }

    private void matchOrder(Order order, OrderBook book) {
        if (order.type == OrderType.BUY) {
            while (order.quantity > 0 && !book.sellOrders.isEmpty() &&
                    book.sellOrders.get(0).price <= order.price) {

                Order sell = book.sellOrders.get(0);
                int qty = Math.min(order.quantity, sell.quantity);
                executeTrade(order, sell, qty);
                sell.quantity -= qty;
                order.quantity -= qty;

                if (sell.quantity == 0) book.sellOrders.remove(0);
            }
            if (order.quantity > 0) book.addOrder(order);
        } else {
            while (order.quantity > 0 && !book.buyOrders.isEmpty() &&
                    book.buyOrders.get(0).price >= order.price) {

                Order buy = book.buyOrders.get(0);
                int qty = Math.min(order.quantity, buy.quantity);
                executeTrade(buy, order, qty);
                buy.quantity -= qty;
                order.quantity -= qty;

                if (buy.quantity == 0) book.buyOrders.remove(0);
            }
            if (order.quantity > 0) book.addOrder(order);
        }
    }

    private void executeTrade(Order buy, Order sell, int qty) {
        double tradeAmount = qty * sell.price;

        User buyer = users.get(buy.userId);
        User seller = users.get(sell.userId);

        buyer.portfolio.balance -= tradeAmount;
        seller.portfolio.balance += tradeAmount;

        buyer.portfolio.addStock(buy.stock, qty);
        seller.portfolio.removeStock(sell.stock, qty);

        System.out.println("Trade Executed: " + buy.stock + ", Qty: " + qty + ", Price: " + sell.price);
    }

    public void viewPortfolio(String userId) {
        User u = users.get(userId);
        System.out.println("\nPortfolio of " + userId);
        System.out.println("Balance: " + u.portfolio.balance);
        System.out.println("Holdings: " + u.portfolio.holdings);
    }
}

public class BrokerageDemo {
    public static void main(String[] args) {
        BrokerageService service = new BrokerageService();

        service.createUser("Sachin");
        service.createUser("Amit");

        service.users.get("Amit").portfolio.addStock("TCS", 50);

        service.placeOrder(new Order("Sachin", "TCS", 20, 3200, OrderType.BUY));
        service.placeOrder(new Order("Amit", "TCS", 15, 3190, OrderType.SELL));

        service.placeOrder(new Order("Sachin", "TCS", 10, 3210, OrderType.BUY));
        service.placeOrder(new Order("Amit", "TCS", 20, 3205, OrderType.SELL));

        service.viewPortfolio("Sachin");
        service.viewPortfolio("Amit");
    }
}
