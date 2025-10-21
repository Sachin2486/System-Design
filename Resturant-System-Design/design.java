import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * FoodDeliverySystem.java
 * - Single-file demo for a food delivery system
 * - Pattern: Entities + Manager/Service classes + Singleton controller + NotificationCenter
 * - Thread-safe and simulates concurrent orders/deliveries
 */

// -------------------- ENUMS & SIMPLE TYPES --------------------
enum OrderStatus { PLACED, PAID, PREPARING, READY_FOR_PICKUP, ASSIGNED, PICKED_UP, DELIVERED, CANCELLED }
enum PaymentMethod { CARD, UPI, CASH }
enum AgentStatus { AVAILABLE, ON_DELIVERY }

// -------------------- ENTITIES --------------------
class Customer {
    final int id;
    final String name;
    final String phone;
    final String address;

    public Customer(int id, String name, String phone, String address) {
        this.id = id; this.name = name; this.phone = phone; this.address = address;
    }

    @Override public String toString() { return name + " (" + phone + ")"; }
}

class Restaurant {
    final int id;
    final String name;
    final String address;
    private final ConcurrentMap<Integer, MenuItem> menu = new ConcurrentHashMap<>();

    public Restaurant(int id, String name, String address) {
        this.id = id; this.name = name; this.address = address;
    }

    void addMenuItem(MenuItem item) { menu.put(item.id, item); }
    void removeMenuItem(int itemId) { menu.remove(itemId); }
    Collection<MenuItem> getMenu() { return menu.values(); }

    @Override public String toString() { return name + " @ " + address; }
}

class MenuItem {
    final int id;
    final String name;
    final double price;
    volatile boolean available;

    public MenuItem(int id, String name, double price, boolean available) {
        this.id = id; this.name = name; this.price = price; this.available = available;
    }

    @Override public String toString() { return name + " ₹" + price + (available ? "" : " (NA)"); }
}

class DeliveryAgent {
    final int id;
    final String name;
    volatile AgentStatus status = AgentStatus.AVAILABLE;

    public DeliveryAgent(int id, String name) { this.id = id; this.name = name; }
    @Override public String toString() { return name + " [" + status + "]"; }
}

class OrderItem {
    final int itemId;
    final int quantity;
    final double unitPrice;

    public OrderItem(int itemId, int quantity, double unitPrice) {
        this.itemId = itemId; this.quantity = quantity; this.unitPrice = unitPrice;
    }

    double subtotal() { return unitPrice * quantity; }
}

class Order {
    final int id;
    final int restaurantId;
    final int customerId;
    final List<OrderItem> items;
    volatile OrderStatus status;
    volatile Integer agentId; // assigned delivery agent
    final PaymentMethod paymentMethod;
    final double totalAmount;
    final long createdAt;

    public Order(int id, int restaurantId, int customerId, List<OrderItem> items, PaymentMethod paymentMethod) {
        this.id = id; this.restaurantId = restaurantId; this.customerId = customerId; this.items = items;
        this.paymentMethod = paymentMethod; this.totalAmount = items.stream().mapToDouble(OrderItem::subtotal).sum();
        this.status = OrderStatus.PLACED; this.agentId = null; this.createdAt = System.currentTimeMillis();
    }

    @Override public String toString() {
        return "Order#" + id + " Rest:" + restaurantId + " Cust:" + customerId + " Total:₹" + totalAmount + " Status:" + status
                + (agentId==null ? "" : " Agent:" + agentId);
    }
}

// -------------------- NOTIFICATION CENTER (Observer-ish) --------------------
class NotificationCenter {
    private static NotificationCenter instance;
    private final ConcurrentMap<Integer, BlockingQueue<String>> userQueues = new ConcurrentHashMap<>();

    private NotificationCenter() {}

    public static synchronized NotificationCenter getInstance() {
        if (instance == null) instance = new NotificationCenter();
        return instance;
    }

    public void registerUser(int userId) {
        userQueues.putIfAbsent(userId, new LinkedBlockingQueue<>());
    }

    public void notifyUser(int userId, String message) {
        userQueues.putIfAbsent(userId, new LinkedBlockingQueue<>());
        userQueues.get(userId).offer("Notification: " + message);
    }

    public List<String> drainNotifications(int userId) {
        BlockingQueue<String> q = userQueues.get(userId);
        if (q == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        q.drainTo(out);
        return out;
    }
}

// -------------------- SERVICES / MANAGERS --------------------

// Auth / User registry (lightweight for demo)
class UserService {
    private final ConcurrentMap<Integer, Customer> customers = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(1);

    public Customer registerCustomer(String name, String phone, String address) {
        int id = idGen.getAndIncrement();
        Customer c = new Customer(id, name, phone, address);
        customers.put(id, c);
        NotificationCenter.getInstance().registerUser(id);
        return c;
    }

    public Optional<Customer> getCustomer(int id) { return Optional.ofNullable(customers.get(id)); }
}

// Restaurant management
class RestaurantService {
    private final ConcurrentMap<Integer, Restaurant> restaurants = new ConcurrentHashMap<>();
    private final AtomicInteger restIdGen = new AtomicInteger(1);
    private final AtomicInteger menuItemIdGen = new AtomicInteger(100);

    public Restaurant registerRestaurant(String name, String address) {
        int id = restIdGen.getAndIncrement();
        Restaurant r = new Restaurant(id, name, address);
        restaurants.put(id, r);
        return r;
    }

    public MenuItem addMenuItem(int restaurantId, String name, double price, boolean available) {
        Restaurant r = restaurants.get(restaurantId);
        if (r == null) throw new IllegalArgumentException("Restaurant not found");
        int mid = menuItemIdGen.getAndIncrement();
        MenuItem item = new MenuItem(mid, name, price, available);
        r.addMenuItem(item);
        return item;
    }

    public Collection<MenuItem> getMenu(int restaurantId) {
        Restaurant r = restaurants.get(restaurantId);
        if (r == null) return Collections.emptyList();
        return r.getMenu();
    }

    public Optional<Restaurant> getRestaurant(int id) { return Optional.ofNullable(restaurants.get(id)); }
}

// Payment service (mock)
class PaymentService {
    public boolean processPayment(Order order) {
        // In production: integrate with gateway, handle idempotency, retries.
        System.out.println("Processing payment for Order#" + order.id + " via " + order.paymentMethod + " amount ₹" + order.totalAmount);
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("Payment successful for Order#" + order.id);
        return true;
    }
}

// Delivery agent manager
class DeliveryAgentService {
    private final ConcurrentMap<Integer, DeliveryAgent> agents = new ConcurrentHashMap<>();
    private final AtomicInteger agentIdGen = new AtomicInteger(1);

    public DeliveryAgent registerAgent(String name) {
        int id = agentIdGen.getAndIncrement();
        DeliveryAgent a = new DeliveryAgent(id, name);
        agents.put(id, a);
        return a;
    }

    public Optional<DeliveryAgent> getAgent(int id) { return Optional.ofNullable(agents.get(id)); }

    // Find any available agent (simple nearest logic could be added)
    public Optional<DeliveryAgent> findAvailableAgent() {
        return agents.values().stream().filter(a -> a.status == AgentStatus.AVAILABLE).findAny();
    }

    public void markAgentOnDelivery(int agentId) {
        DeliveryAgent a = agents.get(agentId);
        if (a != null) a.status = AgentStatus.ON_DELIVERY;
    }

    public void markAgentAvailable(int agentId) {
        DeliveryAgent a = agents.get(agentId);
        if (a != null) a.status = AgentStatus.AVAILABLE;
    }
}

// Order service handles placing and lifecycle (thread-safe)
class OrderService {
    private final RestaurantService restaurantService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final DeliveryAgentService agentService;
    private final NotificationCenter notificationCenter = NotificationCenter.getInstance();

    private final ConcurrentMap<Integer, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger orderIdGen = new AtomicInteger(1);

    // For concurrency control per-restaurant (prevent overselling inventory/availability conflicts)
    private final ConcurrentMap<Integer, Object> restaurantLocks = new ConcurrentHashMap<>();

    // executor for background tasks (order prep, assign agent)
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public OrderService(RestaurantService rs, UserService us, PaymentService ps, DeliveryAgentService das) {
        this.restaurantService = rs; this.userService = us; this.paymentService = ps; this.agentService = das;
    }

    public Order placeOrder(int restaurantId, int customerId, Map<Integer,Integer> itemQty, PaymentMethod paymentMethod) {
        Restaurant rest = restaurantService.getRestaurant(restaurantId).orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        Customer cust = userService.getCustomer(customerId).orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // lock per restaurant to ensure consistent availability checks / modifications
        restaurantLocks.putIfAbsent(restaurantId, new Object());
        synchronized (restaurantLocks.get(restaurantId)) {
            // validate items
            List<OrderItem> items = new ArrayList<>();
            for (Map.Entry<Integer,Integer> e : itemQty.entrySet()) {
                int itemId = e.getKey(); int qty = e.getValue();
                MenuItem menuItem = rest.getMenu().stream().filter(m -> m.id == itemId).findFirst().orElse(null);
                if (menuItem == null || !menuItem.available) throw new IllegalArgumentException("Menu item not available: " + itemId);
                items.add(new OrderItem(itemId, qty, menuItem.price));
            }

            int oid = orderIdGen.getAndIncrement();
            Order order = new Order(oid, restaurantId, customerId, items, paymentMethod);
            orders.put(oid, order);

            System.out.println("Order placed: " + order);
            notificationCenter.notifyUser(customerId, "Your order #" + oid + " has been placed.");

            // process payment async
            executor.submit(() -> processOrder(order));
            return order;
        }
    }

    private void processOrder(Order order) {
        // Payment
        order.status = OrderStatus.PLACED;
        if (paymentService.processPayment(order)) {
            order.status = OrderStatus.PAID;
            notificationCenter.notifyUser(order.customerId, "Payment received for order #" + order.id);
            // Simulate restaurant preparing food
            order.status = OrderStatus.PREPARING;
            notificationCenter.notifyUser(order.restaurantId, "New order #" + order.id + " is preparing");
            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            order.status = OrderStatus.READY_FOR_PICKUP;
            notificationCenter.notifyUser(order.customerId, "Order #" + order.id + " is ready for pickup");
            // assign delivery agent
            assignDeliveryAgent(order);
        } else {
            order.status = OrderStatus.CANCELLED;
            notificationCenter.notifyUser(order.customerId, "Payment failed for order #" + order.id);
        }
    }

    private void assignDeliveryAgent(Order order) {
        // try to find agent multiple times
        Optional<DeliveryAgent> maybeAgent = agentService.findAvailableAgent();
        if (!maybeAgent.isPresent()) {
            // wait and retry a few times (simple retry)
            int retries = 3;
            while (retries-- > 0 && !maybeAgent.isPresent()) {
                try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                maybeAgent = agentService.findAvailableAgent();
            }
        }
        if (!maybeAgent.isPresent()) {
            notificationCenter.notifyUser(order.customerId, "No delivery agents available right now for order #" + order.id);
            return;
        }
        DeliveryAgent agent = maybeAgent.get();
        order.agentId = agent.id;
        order.status = OrderStatus.ASSIGNED;
        agentService.markAgentOnDelivery(agent.id);
        notificationCenter.notifyUser(order.customerId, "Order #" + order.id + " assigned to agent " + agent.name);
        notificationCenter.notifyUser(agent.id, "You have been assigned Order #" + order.id);

        // simulate pickup and delivery in background
        executor.submit(() -> performDelivery(order, agent));
    }

    private void performDelivery(Order order, DeliveryAgent agent) {
        try {
            Thread.sleep(400); // travel to restaurant
            order.status = OrderStatus.PICKED_UP;
            notificationCenter.notifyUser(order.customerId, "Order #" + order.id + " picked up by " + agent.name);

            Thread.sleep(800); // travel to customer
            order.status = OrderStatus.DELIVERED;
            notificationCenter.notifyUser(order.customerId, "Order #" + order.id + " delivered. Enjoy!");
            notificationCenter.notifyUser(order.restaurantId, "Order #" + order.id + " delivered.");

            agentService.markAgentAvailable(agent.id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Optional<Order> getOrder(int orderId) { return Optional.ofNullable(orders.get(orderId)); }

    public List<Order> listOrdersForCustomer(int customerId) {
        return orders.values().stream().filter(o -> o.customerId == customerId).collect(Collectors.toList());
    }

    public void shutdown() {
        executor.shutdown();
    }
}

// -------------------- CONTROLLER (SINGLETON) --------------------
class FoodDeliveryController {
    private static FoodDeliveryController instance;
    final UserService userService = new UserService();
    final RestaurantService restaurantService = new RestaurantService();
    final PaymentService paymentService = new PaymentService();
    final DeliveryAgentService agentService = new DeliveryAgentService();
    final OrderService orderService = new OrderService(restaurantService, userService, paymentService, agentService);
    final NotificationCenter notificationCenter = NotificationCenter.getInstance();

    private FoodDeliveryController() {}

    public static synchronized FoodDeliveryController getInstance() {
        if (instance == null) instance = new FoodDeliveryController();
        return instance;
    }

    public UserService users() { return userService; }
    public RestaurantService restaurants() { return restaurantService; }
    public DeliveryAgentService agents() { return agentService; }
    public OrderService orders() { return orderService; }
    public NotificationCenter notifications() { return notificationCenter; }

    public void shutdown() {
        orderService.shutdown();
    }
}

// -------------------- DEMO / MAIN --------------------
public class FoodDeliverySystem {
    public static void main(String[] args) throws Exception {
        FoodDeliveryController ctrl = FoodDeliveryController.getInstance();

        // Register customers
        Customer alice = ctrl.users().registerCustomer("Alice", "9990011111", "Alpha Street");
        Customer bob = ctrl.users().registerCustomer("Bob", "9990022222", "Beta Street");

        // Register restaurants and menu
        Restaurant r1 = ctrl.restaurants().registerRestaurant("Spice Hub", "Market Road");
        MenuItem m1 = ctrl.restaurants().addMenuItem(r1.id, "Paneer Butter Masala", 220.0, true);
        MenuItem m2 = ctrl.restaurants().addMenuItem(r1.id, "Naan", 30.0, true);

        Restaurant r2 = ctrl.restaurants().registerRestaurant("Green Salad", "Mall Plaza");
        MenuItem m3 = ctrl.restaurants().addMenuItem(r2.id, "Greek Salad", 180.0, true);
        MenuItem m4 = ctrl.restaurants().addMenuItem(r2.id, "Fruit Bowl", 120.0, true);

        // Register delivery agents
        DeliveryAgent agent1 = ctrl.agents().registerAgent("Danny");
        DeliveryAgent agent2 = ctrl.agents().registerAgent("Eve");

        // Place orders concurrently
        ExecutorService clientPool = Executors.newFixedThreadPool(4);

        Runnable orderTask1 = () -> {
            Map<Integer,Integer> items = new HashMap<>();
            items.put(m1.id, 1); items.put(m2.id, 2);
            try {
                Order o = ctrl.orders().placeOrder(r1.id, alice.id, items, PaymentMethod.CARD);
                System.out.println("Placed: " + o);
            } catch (Exception ex) { System.out.println("Order1 failed: " + ex.getMessage()); }
        };

        Runnable orderTask2 = () -> {
            Map<Integer,Integer> items = new HashMap<>();
            items.put(m3.id, 2);
            try {
                Order o = ctrl.orders().placeOrder(r2.id, bob.id, items, PaymentMethod.UPI);
                System.out.println("Placed: " + o);
            } catch (Exception ex) { System.out.println("Order2 failed: " + ex.getMessage()); }
        };

        // Submit tasks concurrently
        clientPool.submit(orderTask1);
        clientPool.submit(orderTask2);

        // Wait some time for flows to progress
        Thread.sleep(2500);

        // Print notifications for users and agents
        System.out.println("\n--- Notifications ---");
        System.out.println("Alice notifications: " + ctrl.notifications().drainNotifications(alice.id));
        System.out.println("Bob notifications: " + ctrl.notifications().drainNotifications(bob.id));
        System.out.println("Agent Danny notifications: " + ctrl.notifications().drainNotifications(agent1.id));
        System.out.println("Agent Eve notifications: " + ctrl.notifications().drainNotifications(agent2.id));

        // List orders for Alice
        System.out.println("\n--- Orders for Alice ---");
        ctrl.orders().listOrdersForCustomer(alice.id).forEach(System.out::println);

        // Clean up
        clientPool.shutdown();
        clientPool.awaitTermination(3, TimeUnit.SECONDS);
        ctrl.shutdown();
    }
}
