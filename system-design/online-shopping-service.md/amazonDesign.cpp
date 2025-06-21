#include <bits/stdc++.h>

using namespace std;

class Product {
    public:
    string productId, name , category;
    double price;
    int stock;
    
    Product(string id, string n , string c, double p,int s) :
    productId(id), name(n), category(c), price(p), stock(s) {}
    
    void reduceStock(int qty) {
        if(stock >= qty) 
        stock -= qty;
    }
};

class User {
public:
    string userId, name, email;
    vector<string> orderHistory;

    User(string id, string n, string e) : userId(id), name(n), email(e) {}
};

class Order {
public:
    string orderId, userId;
    unordered_map<string, int> products; // productId -> quantity
    string status;

    Order(string oid, string uid) : orderId(oid), userId(uid), status("Pending") {}
};

class Cart {
    public:
    unordered_map<string,int> items;
    
    void addItem(const string& productId, int qty) {
        items[productId] += qty;
    }
    
    void removeItem(const string& productId) {
        items.erase(productId);
    }
};

class ShoppingService {
    unordered_map<string, Product*> products;
    unordered_map<string, User*> users;
    unordered_map<string, Order*> orders;
    unordered_map<string, Cart> carts;
    mutex mtx;

public:
    void addProduct(Product* p) {
        lock_guard<mutex> lock(mtx);
        products[p->productId] = p;
    }

    void registerUser(User* u) {
        lock_guard<mutex> lock(mtx);
        users[u->userId] = u;
    }

    vector<Product*> search(const string& keyword) {
        vector<Product*> result;
        for (auto& [id, p] : products) {
            if (p->name.find(keyword) != string::npos || p->category.find(keyword) != string::npos)
                result.push_back(p);
        }
        return result;
    }

    void addToCart(const string& userId, const string& productId, int qty) {
        lock_guard<mutex> lock(mtx);
        if (products.count(productId) && products[productId]->stock >= qty) {
            carts[userId].addItem(productId, qty);
        }
    }

    Order* placeOrder(const string& userId) {
        lock_guard<mutex> lock(mtx);
        if (!users.count(userId)) return nullptr;
        string oid = "O" + to_string(orders.size() + 1);
        Order* order = new Order(oid, userId);

        for (auto& [pid, qty] : carts[userId].items) {
            if (products[pid]->stock >= qty) {
                products[pid]->reduceStock(qty);
                order->products[pid] = qty;
            }
        }
        order->status = "Confirmed";
        orders[oid] = order;
        users[userId]->orderHistory.push_back(oid);
        carts[userId].items.clear();
        return order;
    }

    vector<string> getOrderHistory(const string& userId) {
        if (users.count(userId)) return users[userId]->orderHistory;
        return {};
    }

    string getOrderStatus(const string& orderId) {
        if (orders.count(orderId)) return orders[orderId]->status;
        return "Order not found";
    }
};

int main() {
    ShoppingService service;
    service.addProduct(new Product("P1", "Laptop", "Electronics", 80000, 10));
    service.addProduct(new Product("P2", "Shoes", "Footwear", 2000, 50));

    User* u1 = new User("U1", "Sachin", "sachin@mail.com");
    service.registerUser(u1);

    service.addToCart("U1", "P1", 1);
    Order* o = service.placeOrder("U1");

    if (o) {
        cout << "Order Placed: " << o->orderId << ", Status: " << o->status << endl;
    }

    return 0;
}
