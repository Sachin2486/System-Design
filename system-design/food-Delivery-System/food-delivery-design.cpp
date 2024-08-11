#include <iostream>
#include <vector>
#include <algorithm>
#include <string>

using namespace std;

class Restaurant;
class MenuItem;
class Order;

class Customer {
private:
    string name;
    string address;
    vector<Order> orders;

public:
    Customer(string name, string address) : name(name), address(address) {}

    void browseRestaurants(const vector<Restaurant>& restaurants);
    void viewMenu(const Restaurant& restaurant);
    void placeOrder(Restaurant& restaurant, vector<MenuItem> items);
    void viewOrderStatus(const Order& order);
    vector<Order>& getOrders() {
        return orders;
    } 
};

class MenuItem {
private:
    string name;
    double price;
    bool isAvailable;

public:
    MenuItem(string name, double price, bool isAvailable) : name(name), price(price), isAvailable(isAvailable) {}

    string getName() const { return name; }
    double getPrice() const { return price; }
    bool getAvailability() const { return isAvailable; }

    void setPrice(double newPrice) { price = newPrice; }
    void setAvailability(bool available) { isAvailable = available; }

    void display() const {
        cout << name << " - $" << price << (isAvailable ? " (Available)" : " (Not Available)") << endl;
    }
};

class Restaurant {
private:
    string name;
    string address;
    vector<MenuItem> menu;

public:
    Restaurant(string name, string address) : name(name), address(address) {}

    void addItemToMenu(const MenuItem& item) { menu.push_back(item); }

    void removeItemFromMenu(const string& itemName) {
        menu.erase(remove_if(menu.begin(), menu.end(),
            [&itemName](const MenuItem& item) { return item.getName() == itemName; }), menu.end());
    }

    void updateItemPrice(const string& itemName, double price) {
        for (auto& item : menu) {
            if (item.getName() == itemName) {
                item.setPrice(price);
                break;
            }
        }
    }

    void updateItemAvailability(const string& itemName, bool isAvailable) {
        for (auto& item : menu) {
            if (item.getName() == itemName) {
                item.setAvailability(isAvailable);
                break;
            }
        }
    }

    void showMenu() const {
        for (const auto& item : menu) {
            item.display();
        }
    }

    const vector<MenuItem>& getMenu() const { return menu; }
    string getName() const { return name; }
};

class Order {
private:
    int orderId;
    Customer customer;
    Restaurant restaurant;
    vector<MenuItem> items;
    string status;

public:
    Order(int orderId, Customer& customer, Restaurant& restaurant, vector<MenuItem> items)
        : orderId(orderId), customer(customer), restaurant(restaurant), items(items), status("Pending") {}

    void updateStatus(string newStatus) { status = newStatus; }
    
    void trackOrder() const {
        cout << "Order ID: " << orderId << " is currently " << status << endl;
    }

    string getStatus() const { return status; }
};

class DeliveryAgent {
private:
    string name;
    vector<Order> orders;

public:
    DeliveryAgent(string name) : name(name) {}

    void acceptOrder(Order& order) {
        orders.push_back(order);
        order.updateStatus("Accepted");
        cout << "Order accepted by " << name << endl;
    }

    void fulfillOrder(Order& order) {
        order.updateStatus("Delivered");
        cout << "Order delivered by " << name << endl;
    }

    void updateOrderStatus(Order& order, string status) {
        order.updateStatus(status);
        cout << "Order status updated to " << status << " by " << name << endl;
    }
};

class Payment {
public:
    static void processPayment(const Order& order, string paymentMethod) {
        cout << "Processing payment for Order ID: " << order.getStatus() << " using " << paymentMethod << endl;
        cout << "Payment successful! Enjoy your Meal" << endl;
    }
};

// Customer member functions

void Customer::browseRestaurants(const vector<Restaurant>& restaurants) {
    cout << "Browsing Restaurants:" << endl;
    for (const auto& restaurant : restaurants) {
        cout << restaurant.getName() << endl;
    }
}

void Customer::viewMenu(const Restaurant& restaurant) {
    cout << "Menu for " << restaurant.getName() << ":" << endl;
    restaurant.showMenu();
}

void Customer::placeOrder(Restaurant& restaurant, vector<MenuItem> items) {
    static int orderCounter = 1;
    Order newOrder(orderCounter++, *this, restaurant, items);
    orders.push_back(newOrder);
    cout << "Order placed successfully!" << endl;
}

void Customer::viewOrderStatus(const Order& order) {
    order.trackOrder();
}

int main() {
    // Created some  dummy restaurants to test the system
    Restaurant r1("Pizza HUT", "123 Main Cross RD , BLR");
    r1.addItemToMenu(MenuItem("Margherita Pizza", 8.99, true));
    r1.addItemToMenu(MenuItem("Pepperoni Pizza", 9.99, true));

    Restaurant r2("Burger King", "123 Side Cross RD, BLR");
    r2.addItemToMenu(MenuItem("Classic Burger", 5.99, true));
    r2.addItemToMenu(MenuItem("Cheeseburger", 6.99, true));

    // Create a dummy customer data
    Customer customer1("Sachin 2424", "789 BTM BLR");

    // Browse restaurants and place an order
    vector<Restaurant> restaurants = {r1, r2};
    customer1.browseRestaurants(restaurants);
    customer1.viewMenu(r1);
    customer1.placeOrder(r1, {r1.getMenu()[0], r1.getMenu()[1]});

    // Let's Create a delivery agent to handle the order
    DeliveryAgent agent1("Agent James Bond");

    agent1.acceptOrder(customer1.getOrders()[0]);
    agent1.updateOrderStatus(customer1.getOrders()[0], "In Transit");
    agent1.fulfillOrder(customer1.getOrders()[0]);

    // Processing the  payment done by the customer1 and the mode of payment
    Payment::processPayment(customer1.getOrders()[0], "Credit Card");

    return 0;
}
