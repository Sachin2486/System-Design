// Users & Profiles
// Class: User (manages profile, order history).
// Relation: A User can have multiple Orders.

// Products & Categories
// Class: Product (represents an item for sale).
// Class: Category (groups products).
// Relation: A Category contains many Products.

// Cart & Orders
// Class: ShoppingCart (user adds/removes products).
// Class: Order (finalized cart, linked to user).
// Class: OrderItem (product + quantity inside an order).
// Relation: ShoppingCart has multiple Products, Order has multiple OrderItems.

// Inventory
// Class: Inventory (tracks stock).
// Relation: Each Product has a stock count in Inventory.

// Payments
// Interface: PaymentMethod (polymorphism).
// Implementations: CreditCardPayment, UPIPayment, WalletPayment.

// Admin
// Class: Admin (add/update/remove products, categories)

import java.util.*;

// -------- User & Profile --------
class User {
    private String id;
    private String name;
    private String email;
    private String address;
    private List<Order> orderHistory;
    private ShoppingCart cart;
    
    public User(String id, String name,String email, String address) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.orderHistory = new ArrayList<>();
        this.cart = new ShoppingCart();
    }
    
    public ShoppingCart getCart() { 
        return cart; 
    }
    
    public List<Order> getOrderHistory() { 
        return orderHistory;
    }
    
    public void addOrder(Order order) { 
        orderHistory.add(order); 
    }
}

// -------- Products & Categories --------
class Product {
    private String id;
    private String name;
    private double price;
    private Category category;
    
    public Product(String id, String name, double price, Category category) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public double getPrice() {
        return price;
    }
}

class Category {
    private String name;
    private List<Product> products = new ArrayList<>();

    public Category(String name) { this.name = name; }

    public void addProduct(Product product) { products.add(product); }
    public List<Product> getProducts() { return products; }
}

class Inventory {
    private Map<Product, Integer> stock = new HashMap<>();
    
    public void addStock(Product product, int quantity) {
        stock.put(product, stock.getOrDefault(product,0) + quantity);
    }
    
    public boolean isAvailable(Product product, int quantity) {
        return stock.getOrDefault(product, 0) >= quantity;
    }
    
    public void reduceStock(Product product, int quantity) {
        if(isAvailable(product, quantity)) {
            stock.put(product, stock.get(product) - quantity);
        } else {
            throw new RuntimeException("Insufficient stock for " + product.getName());
        }
    }
}

// -------- Cart & Orders --------
class ShoppingCart {
    private Map<Product, Integer> items = new HashMap<>();
    
    public void addProduct(Product product, int quantity) {
        items.put(product, items.getOrDefault(product,0) + quantity);
    }
    
    public void removeProduct(Product product) {
        items.remove(product);
    }
    
    public Map<Product, Integer> getItems() { return items; }
    
    public double calculateTotal() {
        return items.entrySet().stream()
                .mapToDouble(e -> e.getKey().getPrice() * e.getValue())
                .sum();
    }

    public void clearCart() { items.clear(); }
}

class Order {
    private String id;
    private User user;
    private List<OrderItem> orderItems;
    private double total;
    private String status;

    public Order(String id, User user, Map<Product, Integer> cartItems) {
        this.id = id;
        this.user = user;
        this.orderItems = new ArrayList<>();
        this.total = 0.0;
        for (Map.Entry<Product, Integer> entry : cartItems.entrySet()) {
            orderItems.add(new OrderItem(entry.getKey(), entry.getValue()));
            total += entry.getKey().getPrice() * entry.getValue();
        }
        this.status = "CREATED";
    }

    public double getTotal() { return total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

class OrderItem {
    private Product product;
    private int quantity;

    public OrderItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
}

// -------- Payment --------
interface PaymentMethod {
    boolean pay(double amount);
}

class CreditCardPayment implements PaymentMethod {
    public boolean pay(double amount) {
        System.out.println("Paid " + amount + " using Credit Card.");
        return true;
    }
}

class UPIPayment implements PaymentMethod {
    public boolean pay(double amount) {
        System.out.println("Paid " + amount + " using UPI.");
        return true;
    }
}

class WalletPayment implements PaymentMethod {
    public boolean pay(double amount) {
        System.out.println("Paid " + amount + " using Wallet.");
        return true;
    }
}

// -------- Admin --------
class Admin {
    public void addProductToCategory(Category category, Product product) {
        category.addProduct(product);
    }
}

// -------- Service Layer --------
class ShoppingService {
    private Inventory inventory;

    public ShoppingService(Inventory inventory) {
        this.inventory = inventory;
    }

    public Order placeOrder(User user, PaymentMethod paymentMethod) {
        ShoppingCart cart = user.getCart();

        // Check stock
        for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
            if (!inventory.isAvailable(entry.getKey(), entry.getValue())) {
                throw new RuntimeException("Product not available: " + entry.getKey().getName());
            }
        }

        // Reduce stock
        for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
            inventory.reduceStock(entry.getKey(), entry.getValue());
        }

        // Create order
        Order order = new Order(UUID.randomUUID().toString(), user, cart.getItems());

        // Process payment
        if (paymentMethod.pay(order.getTotal())) {
            order.setStatus("CONFIRMED");
            user.addOrder(order);
            cart.clearCart();
            System.out.println("Order placed successfully!");
        } else {
            order.setStatus("FAILED");
        }

        return order;
    }
}

public class Demo {
    public static void main(String[] args) {
        // ----- Setup -----
        Inventory inventory = new Inventory();
        ShoppingService shoppingService = new ShoppingService(inventory);

        // Categories
        Category electronics = new Category("Electronics");
        Category fashion = new Category("Fashion");

        // Products
        Product phone = new Product("P1", "Smartphone", 15000.0, electronics);
        Product laptop = new Product("P2", "Laptop", 60000.0, electronics);
        Product tshirt = new Product("P3", "T-Shirt", 500.0, fashion);

        // Admin adds products to categories
        Admin admin = new Admin();
        admin.addProductToCategory(electronics, phone);
        admin.addProductToCategory(electronics, laptop);
        admin.addProductToCategory(fashion, tshirt);

        // Add stock
        inventory.addStock(phone, 10);
        inventory.addStock(laptop, 5);
        inventory.addStock(tshirt, 20);

        // User
        User user = new User("U1", "Sachin", "sachin@example.com", "Mumbai");

        // ----- Browsing and Adding to Cart -----
        user.getCart().addProduct(phone, 1);
        user.getCart().addProduct(tshirt, 2);

        System.out.println("Cart Total: " + user.getCart().calculateTotal());

        // ----- Placing Order -----
        PaymentMethod payment = new CreditCardPayment();
        Order order = shoppingService.placeOrder(user, payment);

        System.out.println("Order Status: " + order.getStatus());
        System.out.println("User Order History Count: " + user.getOrderHistory().size());
    }
}



   