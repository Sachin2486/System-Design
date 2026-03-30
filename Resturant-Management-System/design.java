import java.util.*;

interface PaymentStrategy {
    void pay(double amount);
}

class CashPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + "using Cash");
    }
}

class CardPayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + "using Credit Card");
    }
}

class MobilePayment implements PaymentStrategy {
    public void pay(double amount) {
        System.out.println("Paid " + amount + "using Mobile Payment");
    }
}

class Ingredient {
    String name;
    int quantity;
    
    Ingredient(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }
    
    void consume(int qty) {
        if (quantity < qty) 
           throw new RuntimeException("Not enough ingredient: " + name);
        quantity -= qty;
    }
}

class MenuItem {
    String id;
    String name;
    double price;
    Map<String, Integer> ingredientsRequired = new HashMap<>();
    
    MenuItem(String id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }
    
    void addIngredient(String ingredient, int qty) {
        ingredientsRequired.put(ingredient, qty);
    }
}

class Order {
    String orderId;
    List<MenuItem> items = new ArrayList<>();
    double totalAmount = 0;
    String status = "CREATED";

    void addItem(MenuItem item) {
        items.add(item);
        totalAmount += item.price;
    }
}

class Reservation {
    String reservationId;
    String customerName;
    int tableNumber;
    String timeSlot;

    Reservation(String id, String customerName, int tableNumber, String timeSlot) {
        this.reservationId = id;
        this.customerName = customerName;
        this.tableNumber = tableNumber;
        this.timeSlot = timeSlot;
    }
}

class Staff {
    String staffId;
    String name;
    String role;
    String schedule;
    double performanceRating;

    Staff(String id, String name, String role) {
        this.staffId = id;
        this.name = name;
        this.role = role;
    }
}

class ReportService {
    void generateSalesReport(Map<String, Order> orders) {
        double total = 0;
        for (Order o : orders.values()) {
            total += o.totalAmount;
        }
        System.out.println("Total Sales: " + total);
    }

    void generateInventoryReport(Map<String, Ingredient> inventory) {
        System.out.println("Inventory Report:");
        for (Ingredient i : inventory.values()) {
            System.out.println(i.name + " -> " + i.quantity);
        }
    }
}

class RestaurantSystem {

    Map<String, MenuItem> menu = new HashMap<>();
    Map<String, Ingredient> inventory = new HashMap<>();
    Map<String, Order> orders = new HashMap<>();
    Map<String, Reservation> reservations = new HashMap<>();
    Map<String, Staff> staffMembers = new HashMap<>();

    ReportService reportService = new ReportService();

    void addMenuItem(MenuItem item) {
        menu.put(item.id, item);
    }

    void showMenu() {
        System.out.println("\nMENU:");
        for (MenuItem item : menu.values()) {
            System.out.println(item.id + " " + item.name + " - " + item.price);
        }
    }

    void addIngredient(String name, int qty) {
        inventory.put(name, new Ingredient(name, qty));
    }

    void placeOrder(String orderId, List<String> itemIds) {
        Order order = new Order();
        order.orderId = orderId;

        for (String id : itemIds) {
            MenuItem item = menu.get(id);

            // consume ingredients
            for (Map.Entry<String, Integer> entry : item.ingredientsRequired.entrySet()) {
                inventory.get(entry.getKey()).consume(entry.getValue());
            }

            order.addItem(item);
        }

        orders.put(orderId, order);
        System.out.println("Order placed. Total = " + order.totalAmount);
    }

    /* -------- PAYMENT -------- */
    void payBill(String orderId, PaymentStrategy paymentStrategy) {
        Order order = orders.get(orderId);
        paymentStrategy.pay(order.totalAmount);
        order.status = "PAID";
    }

    /* -------- RESERVATION -------- */
    void makeReservation(Reservation reservation) {
        reservations.put(reservation.reservationId, reservation);
        System.out.println("Reservation confirmed for " + reservation.customerName);
    }

    /* -------- STAFF -------- */
    void addStaff(Staff staff) {
        staffMembers.put(staff.staffId, staff);
    }

    /* -------- REPORTS -------- */
    void generateReports() {
        reportService.generateSalesReport(orders);
        reportService.generateInventoryReport(inventory);
    }
}

public class Main {
    public static void main(String[] args) {

        RestaurantSystem system = new RestaurantSystem();

        /* ---------- INVENTORY ---------- */
        system.addIngredient("Cheese", 50);
        system.addIngredient("Bread", 50);
        system.addIngredient("Chicken", 50);

        /* ---------- MENU ---------- */
        MenuItem pizza = new MenuItem("1", "Pizza", 300);
        pizza.addIngredient("Cheese", 2);
        pizza.addIngredient("Bread", 1);

        MenuItem burger = new MenuItem("2", "Burger", 150);
        burger.addIngredient("Bread", 1);
        burger.addIngredient("Chicken", 1);

        system.addMenuItem(pizza);
        system.addMenuItem(burger);

        system.showMenu();

        /* ---------- ORDER ---------- */
        system.placeOrder("O1", List.of("1", "2"));

        /* ---------- PAYMENT ---------- */
        system.payBill("O1", new CardPayment());

        /* ---------- RESERVATION ---------- */
        Reservation r = new Reservation("R1", "Sachin", 5, "7PM");
        system.makeReservation(r);

        /* ---------- STAFF ---------- */
        system.addStaff(new Staff("S1", "John", "Chef"));
        system.addStaff(new Staff("S2", "Mike", "Waiter"));

        /* ---------- REPORTS ---------- */
        system.generateReports();
    }
}

