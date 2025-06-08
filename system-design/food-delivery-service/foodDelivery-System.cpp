#include <iostream>
#include <vector>
#include <unordered_map>
#include <memory>
#include <string>

using namespace std;

enum class PaymentMethod {
    CARD, UPI, CASH
};

enum class OrderStatus {
    PLACED, PREPARING, OUT_FOR_DELIVERY, DELIVERED
};

class MenuItem {
public:
    string name;
    double price;
    bool available;

    MenuItem(string n, double p, bool a = true)
        : name(n), price(p), available(a) {}
};

class Restaurant {
public:
    string name;
    string location;
    vector<MenuItem> menu;

    Restaurant(string n, string loc) : name(n), location(loc) {}

    void addMenuItem(string name, double price) {
        menu.emplace_back(name, price);
    }

    void updateMenuItem(string name, double price, bool available) {
        for (auto& item : menu) {
            if (item.name == name) {
                item.price = price;
                item.available = available;
            }
        }
    }

    void displayMenu() {
        cout << "\nMenu of " << name << ":\n";
        for (auto& item : menu) {
            if (item.available)
                cout << " - " << item.name << ": ₹" << item.price << "\n";
        }
    }
};

class Customer {
public:
    string name;
    string address;

    Customer(string n, string addr) : name(n), address(addr) {}
};

class DeliveryAgent {
public:
    string name;
    bool available;

    DeliveryAgent(string n) : name(n), available(true) {}
};

class Order {
public:
    int id;
    shared_ptr<Customer> customer;
    shared_ptr<Restaurant> restaurant;
    vector<MenuItem> items;
    PaymentMethod paymentMethod;
    OrderStatus status;
    shared_ptr<DeliveryAgent> agent;
    double totalAmount;

    Order(int i, shared_ptr<Customer> c, shared_ptr<Restaurant> r,
          vector<MenuItem> it, PaymentMethod pm)
        : id(i), customer(c), restaurant(r), items(it),
          paymentMethod(pm), status(OrderStatus::PLACED), totalAmount(0.0) {
        for (auto& item : items) totalAmount += item.price;
    }

    void assignAgent(shared_ptr<DeliveryAgent> ag) {
        agent = ag;
        agent->available = false;
        status = OrderStatus::OUT_FOR_DELIVERY;
    }

    void updateStatus(OrderStatus s) {
        status = s;
        if (s == OrderStatus::DELIVERED && agent)
            agent->available = true;
    }

    void display() {
        cout << "\nOrder ID: " << id << " | Customer: " << customer->name
             << "\nRestaurant: " << restaurant->name
             << "\nItems:\n";
        for (auto& item : items)
            cout << " - " << item.name << " ₹" << item.price << "\n";
        cout << "Total: ₹" << totalAmount << "\nStatus: " << statusToStr(status)
             << (agent ? "\nDelivery Agent: " + agent->name : "") << "\n";
    }

private:
    string statusToStr(OrderStatus s) {
        switch (s) {
            case OrderStatus::PLACED: return "Placed";
            case OrderStatus::PREPARING: return "Preparing";
            case OrderStatus::OUT_FOR_DELIVERY: return "Out for Delivery";
            case OrderStatus::DELIVERED: return "Delivered";
        }
        return "Unknown";
    }
};

// --------------------- Managers ---------------------

class RestaurantManager {
public:
    vector<shared_ptr<Restaurant>> restaurants;

    void addRestaurant(shared_ptr<Restaurant> r) {
        restaurants.push_back(r);
    }

    void browseRestaurants() {
        cout << "\nAvailable Restaurants:\n";
        for (auto& r : restaurants) {
            cout << " - " << r->name << " @ " << r->location << "\n";
        }
    }

    shared_ptr<Restaurant> getRestaurant(string name) {
        for (auto& r : restaurants)
            if (r->name == name)
                return r;
        return nullptr;
    }
};

class DeliveryManager {
public:
    vector<shared_ptr<DeliveryAgent>> agents;

    void addAgent(shared_ptr<DeliveryAgent> agent) {
        agents.push_back(agent);
    }

    shared_ptr<DeliveryAgent> assignAgent() {
        for (auto& a : agents) {
            if (a->available) return a;
        }
        return nullptr;
    }
};

class OrderManager {
    int nextOrderId = 1;
    vector<shared_ptr<Order>> orders;

public:
    shared_ptr<Order> placeOrder(shared_ptr<Customer> c, shared_ptr<Restaurant> r,
                                 vector<MenuItem> items, PaymentMethod p) {
        auto order = make_shared<Order>(nextOrderId++, c, r, items, p);
        orders.push_back(order);
        return order;
    }

    void trackOrder(int id) {
        for (auto& o : orders) {
            if (o->id == id) {
                o->display();
                return;
            }
        }
        cout << "Order not found.\n";
    }

    void markDelivered(int id) {
        for (auto& o : orders) {
            if (o->id == id) {
                o->updateStatus(OrderStatus::DELIVERED);
                return;
            }
        }
    }
};

// --------------------- Main System ---------------------

class FoodDeliverySystem {
    RestaurantManager restaurantMgr;
    OrderManager orderMgr;
    DeliveryManager deliveryMgr;
    vector<shared_ptr<Customer>> customers;

public:
    void setup() {
        // Sample data
        auto r1 = make_shared<Restaurant>("Biryani House", "Mumbai");
        r1->addMenuItem("Chicken Biryani", 250);
        r1->addMenuItem("Paneer Biryani", 220);

        auto r2 = make_shared<Restaurant>("Pizza Palace", "Mumbai");
        r2->addMenuItem("Margherita", 300);
        r2->addMenuItem("Farmhouse", 400);

        restaurantMgr.addRestaurant(r1);
        restaurantMgr.addRestaurant(r2);

        deliveryMgr.addAgent(make_shared<DeliveryAgent>("Raju"));
        deliveryMgr.addAgent(make_shared<DeliveryAgent>("Seema"));
    }

    shared_ptr<Customer> createCustomer(string name, string address) {
        auto c = make_shared<Customer>(name, address);
        customers.push_back(c);
        return c;
    }

    void start() {
        setup();
        restaurantMgr.browseRestaurants();

        auto cust = createCustomer("Sachin", "Andheri West");

        auto rest = restaurantMgr.getRestaurant("Biryani House");
        rest->displayMenu();

        vector<MenuItem> orderItems = { rest->menu[0], rest->menu[1] };
        auto order = orderMgr.placeOrder(cust, rest, orderItems, PaymentMethod::UPI);

        cout << "\nOrder Placed:\n";
        order->display();

        auto agent = deliveryMgr.assignAgent();
        if (agent) {
            order->assignAgent(agent);
            cout << "\nAgent Assigned.\n";
        }

        orderMgr.trackOrder(order->id);
        orderMgr.markDelivered(order->id);

        cout << "\nAfter Delivery:\n";
        orderMgr.trackOrder(order->id);
    }
};

// --------------------- Main ---------------------

int main() {
    FoodDeliverySystem system;
    system.start();
    return 0;
}
