import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    PICKED_UP,
    DELIVERED,
    CANCELLED
}

enum PaymentMethod {
    UPI,
    CARD,
    CASH
}

enum AgentStatus {
    AVAILABLE,
    BUSY,
    OFFLINE
}

class Customer {

    private final int id;
    private final String name;

    public Customer(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

class MenuItem {

    private final int id;
    private final String name;
    private double price;
    private boolean available;

    public MenuItem(
            int id,
            String name,
            double price
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.available = true;
    }

    public boolean isAvailable() {
        return available;
    }

    public double getPrice() {
        return price;
    }

    public String getName() {
        return name;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}

class Restaurant {

    private final int id;
    private final String name;

    private final List<MenuItem> menu =
            new CopyOnWriteArrayList<>();

    public Restaurant(
            int id,
            String name
    ) {
        this.id = id;
        this.name = name;
    }

    public void addMenuItem(MenuItem item) {
        menu.add(item);
    }

    public List<MenuItem> getMenu() {
        return menu;
    }

    public String getName() {
        return name;
    }
}

class DeliveryAgent {

    private final int id;
    private final String name;
    private AgentStatus status;

    public DeliveryAgent(
            int id,
            String name
    ) {
        this.id = id;
        this.name = name;
        this.status = AgentStatus.AVAILABLE;
    }

    public AgentStatus getStatus() {
        return status;
    }

    public void setStatus(
            AgentStatus status
    ) {
        this.status = status;
    }

    public String getName() {
        return name;
    }
}

class Order {

    private final int orderId;

    private final Customer customer;
    private final Restaurant restaurant;
    private final List<MenuItem> items;

    private DeliveryAgent agent;

    private OrderStatus status;

    private final double totalAmount;

    public Order(
            int orderId,
            Customer customer,
            Restaurant restaurant,
            List<MenuItem> items,
            double totalAmount
    ) {
        this.orderId = orderId;
        this.customer = customer;
        this.restaurant = restaurant;
        this.items = items;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PLACED;
    }

    public void assignAgent(
            DeliveryAgent agent
    ) {
        this.agent = agent;
    }

    public void updateStatus(
            OrderStatus status
    ) {
        this.status = status;
    }

    public int getOrderId() {
        return orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }
}

class PaymentService {

    public boolean processPayment(
            double amount,
            PaymentMethod method
    ) {

        System.out.println(
                "Payment of " + amount +
                        " via " + method
        );

        return true;
    }
}

class NotificationService {

    public void notifyUser(
            String user,
            String msg
    ) {
        System.out.println(
                "[NOTIFICATION] " +
                        user +
                        " -> " +
                        msg
        );
    }
}

class OrderService {

    private final Map<Integer, Order> orders =
            new ConcurrentHashMap<>();

    private final List<DeliveryAgent> agents;

    private final PaymentService paymentService =
            new PaymentService();

    private final NotificationService notificationService =
            new NotificationService();

    private final AtomicInteger orderIdGen =
            new AtomicInteger(1);

    public OrderService(
            List<DeliveryAgent> agents
    ) {
        this.agents = agents;
    }

    public Order placeOrder(
            Customer customer,
            Restaurant restaurant,
            List<MenuItem> items,
            PaymentMethod paymentMethod
    ) {

        double total = 0;

        for (MenuItem item : items) {

            if (!item.isAvailable()) {
                throw new RuntimeException(
                        item.getName() +
                                " unavailable"
                );
            }

            total += item.getPrice();
        }

        paymentService.processPayment(
                total,
                paymentMethod
        );

        Order order =
                new Order(
                        orderIdGen.getAndIncrement(),
                        customer,
                        restaurant,
                        items,
                        total
                );

        assignDeliveryAgent(order);

        orders.put(
                order.getOrderId(),
                order
        );

        notificationService.notifyUser(
                customer.getName(),
                "Order placed successfully"
        );

        return order;
    }

    private void assignDeliveryAgent(
            Order order
    ) {

        for (DeliveryAgent agent : agents) {

            if (agent.getStatus() ==
                    AgentStatus.AVAILABLE) {

                agent.setStatus(
                        AgentStatus.BUSY
                );

                order.assignAgent(agent);

                notificationService.notifyUser(
                        agent.getName(),
                        "New delivery assigned"
                );

                return;
            }
        }

        throw new RuntimeException(
                "No delivery agent available"
        );
    }

    public void updateOrderStatus(
            int orderId,
            OrderStatus status
    ) {

        Order order = orders.get(orderId);

        if (order == null) {
            throw new RuntimeException(
                    "Order not found"
            );
        }

        order.updateStatus(status);

        System.out.println(
                "Order " +
                        orderId +
                        " -> " +
                        status
        );
    }

    public OrderStatus trackOrder(
            int orderId
    ) {

        Order order = orders.get(orderId);

        if (order == null) {
            throw new RuntimeException(
                    "Order not found"
            );
        }

        return order.getStatus();
    }
}

public class FoodDeliveryDemo {

    public static void main(String[] args) {

        Restaurant restaurant =
                new Restaurant(
                        1,
                        "Dominos"
                );

        MenuItem pizza =
                new MenuItem(
                        1,
                        "Pizza",
                        250
                );

        MenuItem coke =
                new MenuItem(
                        2,
                        "Coke",
                        50
                );

        restaurant.addMenuItem(pizza);
        restaurant.addMenuItem(coke);

        DeliveryAgent a1 =
                new DeliveryAgent(
                        1,
                        "Rohit"
                );

        DeliveryAgent a2 =
                new DeliveryAgent(
                        2,
                        "Sachin"
                );

        List<DeliveryAgent> agents =
                Arrays.asList(a1, a2);

        OrderService orderService =
                new OrderService(agents);

        Customer customer =
                new Customer(
                        1,
                        "Kiran"
                );

        Order order =
                orderService.placeOrder(
                        customer,
                        restaurant,
                        Arrays.asList(
                                pizza,
                                coke
                        ),
                        PaymentMethod.UPI
                );

        orderService.updateOrderStatus(
                order.getOrderId(),
                OrderStatus.CONFIRMED
        );

        orderService.updateOrderStatus(
                order.getOrderId(),
                OrderStatus.PREPARING
        );

        System.out.println(
                orderService.trackOrder(
                        order.getOrderId()
                )
        );
    }
}