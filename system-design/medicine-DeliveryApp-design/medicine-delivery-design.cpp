#include <bits/stdc++.h>
using namespace std;

class Medicine {
public:
    int id;
    string name;
    double price;
    int quantity;

    Medicine() : id(0), name(""), price(0.0), quantity(0) {}

    Medicine(int id, string name, double price, int quantity)
        : id(id), name(name), price(price), quantity(quantity) {}

    void display() const {
        cout << "ID: " << id << ", Name: " << name << ", Price: " << price << ", Quantity: " << quantity << "\n";
    }
};

class Cart {
private:
    unordered_map<int, int> cartItems;

public:
    void addToCart(int medicineId, int quantity) {
        cartItems[medicineId] += quantity;
    }

    void displayCart(const unordered_map<int, Medicine>& inventory) {
        cout << "Cart items:\n";
        for (const auto& item : cartItems) {
            int medicineId = item.first;
            int quantity = item.second;
            if (inventory.find(medicineId) != inventory.end()) {
                const Medicine& med = inventory.at(medicineId);
                cout << "Medicine: " << med.name << ", Quantity: " << quantity
                     << ", Total Price: " << quantity * med.price << "\n";
            } else {
                cout << "Medicine ID " << medicineId << " not found in inventory.\n";
            }
        }
    }
};

class Order {
private:
    Cart cart;
    string status;

public:
    Order(const Cart& cart) : cart(cart), status("pending") {}

    void makePayment() {
        cout << "Payment made successfully.\n";
    }

    void updateDeliveryStatus(const string& newStatus) {
        status = newStatus;
        cout << "Delivery status updated to: " << status << "\n";
    }
};

class MedicineDeliverySystem {
private:
    unordered_map<int, Medicine> inventory;
    vector<Order> orders;

public:
    void addMedicineToInventory(const Medicine& medicine) {
        inventory[medicine.id] = medicine;
    }

    void placeOrder(const Cart& cart) {
        orders.push_back(Order(cart));
    }

    // Getter for inventory
    const unordered_map<int, Medicine>& getInventory() const {
        return inventory;
    }

    // Getter for orders
    vector<Order>& getOrders() {
        return orders;
    }
};

int main() {
    MedicineDeliverySystem system;

    // Adding medicines to inventory
    Medicine med1(1, "Paracetamol", 50.0, 100);
    Medicine med2(2, "Amoxicillin", 120.0, 50);
    system.addMedicineToInventory(med1);
    system.addMedicineToInventory(med2);

    // Creating a cart and adding items to it
    Cart cart;
    cart.addToCart(1, 2); // 2 units of Paracetamol
    cart.addToCart(2, 1); // 1 unit of Amoxicillin

    // Display the cart with system inventory
    cart.displayCart(system.getInventory());

    // Placing an order
    system.placeOrder(cart);

    // Access orders via getter method
    system.getOrders()[0].makePayment();
    system.getOrders()[0].updateDeliveryStatus("Shipped");

    return 0;
}
