#include<bits/stdc++.h>

using namespace std;

enum class OrderState { CREATED, CONFIRMED, CANCELLED, FULFILLED };

struct Item {
    string itemId;
    double pricePerUnit;
    int quantity;
};

struct OrderItem {
    string itemId;
    int quantity;
};

struct Order {
    string orderId;
    string customerId;
    string address;
    string seller;
    vector<OrderItem> items;
    OrderState state;
    double totalAmount;
};

class InternalInventory {
    unordered_map<string,Item> items;
    
    public:
    void addItemToInventory(const string& itemId, int quantity, double price) {
        if(items.count(itemId)) {
            items[itemId].quantity += quantity;
        } else {
            items[itemId] = {itemId, quantity, price};
        }
    }
    
    int getAvailableQuantity(const string& itemId) {
        if(!items.count(itemId)) return 0;
        return items[itemId].quantity;
    }
    
    double getPricePerUnit(const string& itemId) {
        if (!items.count(itemId)) throw runtime_error("Item not found");
        return items[itemId].pricePerUnit;
    }
    
    void reserveItem(const string& itemId, int quantity) {
        if (items[itemId].quantity < quantity) throw runtime_error("Insufficient stock");
        items[itemId].quantity -= quantity;
    }

    void releaseItem(const string& itemId, int quantity) {
        items[itemId].quantity += quantity;
    }
};

class ExternalInventoryAPI {
  unordered_map<string, int> stock = {
        {"item_ext_1", 100},
        {"item_ext_2", 50}
    };
    unordered_map<string, double> price = {
        {"item_ext_1", 150.0},
        {"item_ext_2", 99.0}
    };
    
    public:
    int getAvailableInventory(const string& itemId) {
        return stock[itemId];
    }

    double getPrice(const string& itemId) {
        return price[itemId];
    }

    void reserveItem(const string& itemId, int quantity) {
        if (stock[itemId] < quantity) throw runtime_error("External stock insufficient");
        stock[itemId] -= quantity;
    }

    void releaseItem(const string& itemId, int quantity) {
        stock[itemId] += quantity;
    }
};

class OrderManagementSystem {
    InternalInventory internalInventory;
    ExternalInventoryAPI externalAPI;
    unordered_map<string, Order> orders;
    int orderCounter = 1;
    
    public:
    void addItemToInventory(const string& itemId, int quantity, double price) {
        internalInventory.addItemToInventory(itemId, quantity, price);
    }
    
    int getAvailableInventory(const string& itemId, const string& seller) {
        if (seller == "INTERNAL") return internalInventory.getAvailableQuantity(itemId);
        if (seller == "EXTERNAL") return externalAPI.getAvailableInventory(itemId);
        throw runtime_error("Invalid seller");
    }
    
    string createOrder(const string& customerId, const vector<OrderItem>& items, const string& address, const string& seller) {
        double total = 0;
        for (auto& item : items) {
            if (seller == "INTERNAL") {
                internalInventory.reserveItem(item.itemId, item.quantity);
                total += item.quantity * internalInventory.getPricePerUnit(item.itemId);
            } else if (seller == "EXTERNAL") {
                externalAPI.reserveItem(item.itemId, item.quantity);
                total += item.quantity * externalAPI.getPrice(item.itemId);
            } else {
                throw runtime_error("Invalid seller");
            }
        }

        string orderId = "ORD" + to_string(orderCounter++);
        orders[orderId] = {orderId, customerId, address, seller, items, OrderState::CREATED, total};
        cout << "Order Created: " << orderId << ", Total = Rs. " << total << endl;
        return orderId;
    }
    
    void updateOrder(const string& orderId, OrderState newState) {
        if (!orders.count(orderId)) throw runtime_error("Order not found");
        Order& order = orders[orderId];

        if (newState == OrderState::CONFIRMED) {
            if (order.state != OrderState::CREATED)
                throw runtime_error("Only created orders can be confirmed");
            order.state = OrderState::CONFIRMED;
        } else if (newState == OrderState::CANCELLED) {
            if (order.state == OrderState::CANCELLED || order.state == OrderState::FULFILLED)
                throw runtime_error("Cannot cancel completed order");
            // release inventory
            for (auto& item : order.items) {
                if (order.seller == "INTERNAL") {
                    internalInventory.releaseItem(item.itemId, item.quantity);
                } else {
                    externalAPI.releaseItem(item.itemId, item.quantity);
                }
            }
            order.state = OrderState::CANCELLED;
        } else if (newState == OrderState::FULFILLED) {
            if (order.state != OrderState::CONFIRMED)
                throw runtime_error("Only confirmed orders can be fulfilled");
            order.state = OrderState::FULFILLED;
        } else {
            throw runtime_error("Invalid state transition");
        }

        cout << "Order " << orderId << " updated to state: " << static_cast<int>(newState) << endl;
    }
};

int main() {
    OrderManagementSystem system;
    
     // Add Internal Inventory
    system.addItemToInventory("item_int_1", 10, 100);
    system.addItemToInventory("item_int_2", 5, 250);
    
    vector<OrderItem> items1 = { {"item_int_1", 2}, {"item_int_2", 1} };
    string oid1 = system.createOrder("cust123", items1, "Bangalore", "INTERNAL");
    
    // Confirm the order
    system.updateOrder(oid1, OrderState::CONFIRMED);

    // Create and cancel an external order
    vector<OrderItem> items2 = { {"item_ext_1", 3} };
    string oid2 = system.createOrder("cust456", items2, "Mumbai", "EXTERNAL");
    system.updateOrder(oid2, OrderState::CANCELLED);

    return 0;
}