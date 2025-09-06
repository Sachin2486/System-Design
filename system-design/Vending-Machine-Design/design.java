// Product → represents an item in the machine (name, price, quantity).
// Inventory → manages all products (add, update, check availability).
// Payment → handles coin/note insertion, calculates total amount, returns change.

// Transaction → represents one purchase (selected product, money inserted, status).

// VendingMachine → main system (facade) to manage products, payments, and transactions.

// Admin → interface for restocking and collecting cash.


import java.util.*;

class Product {
    private String name;
    private double price;
    private int quantity;
    
    public Product(String name, double price, int quantity) {
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
    
    public String getName() {
        return name;
    }
    
    public double getPrice() {
        return price;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void reduceQuantity() {
        if(quantity > 0)
        quantity--;
    }
    
    public void addQuantity(int qty){
        this.quantity += qty;
    }
    
    @Override
    public String toString() {
        return name + " (₹" + price + ") - Qty: " + quantity;
    }
}

// --- Inventory ---
class Inventory {
    private Map<String, Product> products;

    public Inventory() {
        this.products = new HashMap<>();
    }

    public void addProduct(Product product) {
        products.put(product.getName(), product);
    }

    public Product getProduct(String name) {
        return products.get(name);
    }

    public void restock(String name, int qty) {
        if (products.containsKey(name)) {
            products.get(name).addQuantity(qty);
        }
    }

    public void showProducts() {
        System.out.println("Available Products:");
        for (Product p : products.values()) {
            System.out.println(p);
        }
    }
}

class Payment {
    private double totalInserted;

    public void insertMoney(double amount) {
        totalInserted += amount;
        System.out.println("Inserted ₹" + amount + ". Total: ₹" + totalInserted);
    }

    public double getTotalInserted() {
        return totalInserted;
    }

    public double returnChange(double productPrice) {
        double change = totalInserted - productPrice;
        totalInserted = 0; // reset after transaction
        return change;
    }

    public void reset() {
        totalInserted = 0;
    }
}

class Transaction {
    private static int counter = 1;
    private int transactionId;
    private Product product;
    private double amountPaid;
    private boolean successful;
    
    public Transaction(Product product, double amountPaid, boolean successful) {
        this.transactionId = counter++;
        this.product = product;
        this.amountPaid = amountPaid;
        this.successful = successful;
    }
    
    @Override
    public String toString() {
        return "Transaction #" + transactionId + " -> " + product.getName() +
                " | Paid: ₹" + amountPaid + " | Status: " + (successful ? "SUCCESS" : "FAILED");
    }
}

// --- VendingMachine ---
class VendingMachine {
    private Inventory inventory;
    private Payment payment;
    private List<Transaction> transactions;
    private double collectedMoney;

    public VendingMachine() {
        this.inventory = new Inventory();
        this.payment = new Payment();
        this.transactions = new ArrayList<>();
        this.collectedMoney = 0;
    }

    // Customer actions
    public synchronized void insertMoney(double amount) {
        payment.insertMoney(amount);
    }

    public synchronized void selectProduct(String productName) {
        Product product = inventory.getProduct(productName);
        if (product == null) {
            System.out.println("Invalid product selection.");
            return;
        }

        if (product.getQuantity() <= 0) {
            System.out.println("Sorry, " + productName + " is out of stock.");
            transactions.add(new Transaction(product, payment.getTotalInserted(), false));
            payment.reset();
            return;
        }

        if (payment.getTotalInserted() < product.getPrice()) {
            System.out.println("Insufficient funds. Please insert more money.");
            return;
        }

        // Process successful purchase
        product.reduceQuantity();
        double change = payment.returnChange(product.getPrice());
        collectedMoney += product.getPrice();

        System.out.println("Dispensing: " + product.getName());
        if (change > 0) {
            System.out.println("Returning change: ₹" + change);
        }

        transactions.add(new Transaction(product, product.getPrice(), true));
    }

    // Admin actions
    public void restockProduct(String productName, int qty) {
        inventory.restock(productName, qty);
        System.out.println("Restocked " + productName + " by " + qty + " units.");
    }

    public void collectMoney() {
        System.out.println("Admin collected ₹" + collectedMoney);
        collectedMoney = 0;
    }

    public void showInventory() {
        inventory.showProducts();
    }
}

// --- Demo ---
public class VendingMachineDemo {
    public static void main(String[] args) {
        VendingMachine vm = new VendingMachine();

        // Add products
        vm.restockProduct("Coke", 10); // first restock before adding
        vm.restockProduct("Pepsi", 5);
        vm.restockProduct("Chips", 7);

        // Actually add products
        vm.showInventory(); // Show inventory to user

        // Customer buys a product
        vm.insertMoney(50);
        vm.selectProduct("Coke");

        // Customer with insufficient funds
        vm.insertMoney(10);
        vm.selectProduct("Pepsi");

        // Admin restocks
        vm.restockProduct("Chips", 5);
        vm.showInventory();

        // Admin collects money
        vm.collectMoney();
    }
}


