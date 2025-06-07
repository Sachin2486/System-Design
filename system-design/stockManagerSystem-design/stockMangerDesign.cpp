#include <iostream>
#include <string>
#include <unordered_map>
#include <vector>
#include <memory>
#include <mutex>

using namespace std;

// Forward declaration
class Market;

// ---------------------- Entity Classes ----------------------

class Stock {
public:
    string symbol;
    string name;
    double price;

    Stock(string sym, string nm, double pr) : symbol(sym), name(nm), price(pr) {}
};

class Transaction {
public:
    string stockSymbol;
    int quantity;
    double price;
    string type; // "BUY" or "SELL"
    string timestamp;

    Transaction(string s, int q, double p, string t, string time)
        : stockSymbol(s), quantity(q), price(p), type(t), timestamp(time) {}
};

class Portfolio {
    unordered_map<string, int> holdings;

public:
    void update(string symbol, int qty) {
        holdings[symbol] += qty;
        if (holdings[symbol] == 0)
            holdings.erase(symbol);
    }

    void display() {
        cout << "Your Portfolio:\n";
        for (auto& [symbol, qty] : holdings) {
            cout << " - " << symbol << ": " << qty << " shares\n";
        }
    }

    int getStockQuantity(string symbol) {
        return holdings.count(symbol) ? holdings[symbol] : 0;
    }
};

class UserAccount {
public:
    string username;
    double balance;
    Portfolio portfolio;
    vector<Transaction> history;

    UserAccount(string uname, double bal) : username(uname), balance(bal) {}

    void displayTransactions() {
        cout << "Transaction History:\n";
        for (auto& t : history) {
            cout << t.timestamp << " | " << t.type << " | " << t.stockSymbol
                 << " | Qty: " << t.quantity << " | Price: ₹" << t.price << "\n";
        }
    }
};

// ---------------------- Market Simulator ----------------------

class Market {
    unordered_map<string, shared_ptr<Stock>> stocks;
    mutex mtx; // Protect market updates

public:
    void addStock(string symbol, string name, double price) {
        lock_guard<mutex> lock(mtx);
        stocks[symbol] = make_shared<Stock>(symbol, name, price);
    }

    shared_ptr<Stock> getStock(string symbol) {
        lock_guard<mutex> lock(mtx);
        if (stocks.count(symbol)) return stocks[symbol];
        return nullptr;
    }

    void updatePrice(string symbol, double newPrice) {
        lock_guard<mutex> lock(mtx);
        if (stocks.count(symbol)) stocks[symbol]->price = newPrice;
    }

    void displayMarket() {
        cout << "\nMarket Data:\n";
        for (auto& [_, stock] : stocks) {
            cout << stock->symbol << " (" << stock->name << ") - ₹" << stock->price << "\n";
        }
    }
};

// ---------------------- Brokerage Engine ----------------------

class BrokerageSystem {
    unordered_map<string, shared_ptr<UserAccount>> users;
    Market market;
    mutex mtx; // Protect user/account data

public:
    void createUser(string username, double balance) {
        lock_guard<mutex> lock(mtx);
        users[username] = make_shared<UserAccount>(username, balance);
        cout << "Account created for " << username << " with balance ₹" << balance << "\n";
    }

    void addStockToMarket(string symbol, string name, double price) {
        market.addStock(symbol, name, price);
    }

    void viewMarket() {
        market.displayMarket();
    }

    void viewPortfolio(string username) {
        auto user = getUser(username);
        if (user) user->portfolio.display();
    }

    void viewTransactions(string username) {
        auto user = getUser(username);
        if (user) user->displayTransactions();
    }

    void buyStock(string username, string symbol, int quantity, string timestamp) {
        auto user = getUser(username);
        auto stock = market.getStock(symbol);

        if (!user || !stock) {
            cout << "Invalid user or stock\n";
            return;
        }

        double cost = stock->price * quantity;
        if (user->balance < cost) {
            cout << "Insufficient balance\n";
            return;
        }

        user->balance -= cost;
        user->portfolio.update(symbol, quantity);
        user->history.emplace_back(symbol, quantity, stock->price, "BUY", timestamp);
        cout << "Stock purchased: " << quantity << " shares of " << symbol << "\n";
    }

    void sellStock(string username, string symbol, int quantity, string timestamp) {
        auto user = getUser(username);
        auto stock = market.getStock(symbol);

        if (!user || !stock) {
            cout << "Invalid user or stock\n";
            return;
        }

        int owned = user->portfolio.getStockQuantity(symbol);
        if (owned < quantity) {
            cout << "Not enough shares to sell\n";
            return;
        }

        double gain = stock->price * quantity;
        user->balance += gain;
        user->portfolio.update(symbol, -quantity);
        user->history.emplace_back(symbol, quantity, stock->price, "SELL", timestamp);
        cout << "Stock sold: " << quantity << " shares of " << symbol << "\n";
    }

    void showBalance(string username) {
        auto user = getUser(username);
        if (user) cout << "Balance: ₹" << user->balance << "\n";
    }

private:
    shared_ptr<UserAccount> getUser(const string& username) {
        lock_guard<mutex> lock(mtx);
        if (users.count(username)) return users[username];
        return nullptr;
    }
};

// ---------------------- Main Test ----------------------

int main() {
    BrokerageSystem system;

    system.createUser("sachin", 50000);
    system.addStockToMarket("TCS", "Tata Consultancy Services", 3500);
    system.addStockToMarket("INFY", "Infosys", 1400);
    system.viewMarket();

    system.buyStock("sachin", "TCS", 5, "2025-06-04 10:00");
    system.buyStock("sachin", "INFY", 10, "2025-06-04 10:05");

    system.viewPortfolio("sachin");
    system.showBalance("sachin");

    system.sellStock("sachin", "INFY", 5, "2025-06-04 11:00");
    system.viewTransactions("sachin");
    system.viewPortfolio("sachin");
    system.showBalance("sachin");

    return 0;
}
