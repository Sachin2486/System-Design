import java.util.*;

// Enum representing different product categories
enum ProductType {
	BOOK, CALENDAR, CLOCK, PEN
}

// Product class representing a generic product
class Product {
    private String name;
    private ProductType type;
    private double price;
    
    public Product(String name, ProductType type, double price) {
        this.name = name;
        this.type = type;
        this.price = price;
    }
    
    public String getName() {
        return name;
    }
    
    public double getPrice() {
        return price;
    }
    
    public ProductType getType() {
        return type;
    }
}

// CartItem represents a product with a specific quantity
class CartItem {
    private Product product;
    private int quantity;
    
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }
    
    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }
    
    public void increaseQuantity(int qty) {
        quantity += qty;
    }
    
    public void decreaseQuantity(int qty) {
        this.quantity = Math.max(0, this.quantity - qty);
    }

    public double getTotalPrice() {
        return product.getPrice() * quantity;
    }
}

// Discount strategy interface (Open for extension - Strategy Pattern)
interface DiscountStrategy {
    double applyDiscount(double totalAmount);
}

// No discount implementation
class NoDiscount implements DiscountStrategy {
    public double applyDiscount(double totalAmount) {
        return totalAmount;
    }
}

// Percentage-based discount implementation
class PercentageDiscount implements DiscountStrategy {
    private double percent;

    public PercentageDiscount(double percent) {
        this.percent = percent;
    }

    public double applyDiscount(double totalAmount) {
        return totalAmount - (totalAmount * percent / 100);
    }
}

// Shopping Cart class
class ShoppingCart {
    private Map<String, CartItem> cartItems;
    private DiscountStrategy discountStrategy;

    public ShoppingCart() {
        this.cartItems = new HashMap<>();
        this.discountStrategy = new NoDiscount(); // default
    }

    // Add item to cart
    public void addItem(Product product, int quantity) {
        if (cartItems.containsKey(product.getName())) {
            cartItems.get(product.getName()).increaseQuantity(quantity);
        } else {
            cartItems.put(product.getName(), new CartItem(product, quantity));
        }
        System.out.println(quantity + " x " + product.getName() + " added to cart.");
    }

    // Remove item or quantity
    public void removeItem(String productName, int quantity) {
        if (!cartItems.containsKey(productName)) {
            System.out.println("Item not found in cart!");
            return;
        }
        CartItem item = cartItems.get(productName);
        item.decreaseQuantity(quantity);

        if (item.getQuantity() == 0) {
            cartItems.remove(productName);
            System.out.println(productName + " removed from cart.");
        } else {
            System.out.println(quantity + " quantity removed from " + productName);
        }
    }

    // Apply a discount strategy
    public void applyDiscount(DiscountStrategy strategy) {
        this.discountStrategy = strategy;
    }

    // Calculate total
    public double calculateTotal() {
        double total = 0;
        for (CartItem item : cartItems.values()) {
            total += item.getTotalPrice();
        }
        return discountStrategy.applyDiscount(total);
    }

    // Display cart details
    public void displayCart() {
        if (cartItems.isEmpty()) {
            System.out.println("🛒 Your cart is empty.");
            return;
        }
        System.out.println("\n🛒 Cart Details:");
        for (CartItem item : cartItems.values()) {
            System.out.println("- " + item.getProduct().getName() +
                    " | Qty: " + item.getQuantity() +
                    " | Unit Price: ₹" + item.getProduct().getPrice() +
                    " | Total: ₹" + item.getTotalPrice());
        }
        System.out.println("Cart Total (after discount): ₹" + calculateTotal());
    }
}

// Entry point for the application
public class ShoppingApp {
    public static void main(String[] args) {
        Product book = new Product("Java Book", ProductType.BOOK, 500);
        Product pen = new Product("Fountain Pen", ProductType.PEN, 50);
        Product clock = new Product("Table Clock", ProductType.CLOCK, 350);
        Product calendar = new Product("Desk Calendar", ProductType.CALENDAR, 120);

        ShoppingCart cart = new ShoppingCart();

        cart.addItem(book, 2);
        cart.addItem(pen, 3);
        cart.addItem(clock, 1);
        cart.displayCart();

        cart.removeItem("Fountain Pen", 1);
        cart.displayCart();

        System.out.println("\nApplying 10% Discount on Cart...");
        cart.applyDiscount(new PercentageDiscount(10));
        cart.displayCart();
    }
}