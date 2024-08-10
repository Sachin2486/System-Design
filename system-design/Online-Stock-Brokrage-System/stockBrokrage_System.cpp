#include <iostream>
#include <unordered_map>
#include <vector>
#include <mutex>

using namespace std;

class Stock {
private:
    string symbol;
    double price;

public:
    // Default constructor
    Stock() : symbol(""), price(0.0) {}

    Stock(const string& symbol, double price) : symbol(symbol), price(price) {}

    string getSymbol() const {
        return symbol;
    }

    double getPrice() const {
        return price;
    }

    void setPrice(double newPrice) {
        price = newPrice;
    }
};

class Transaction {
private:
    string type;
    string symbol;
    int quantity;
    double price;

public:
    Transaction(const string& type, const string& symbol, int quantity, double price)
        : type(type), symbol(symbol), quantity(quantity), price(price) {}

    void printTransaction() const {
        cout << type << " " << quantity << " shares of " << symbol << " at $" << price << endl;
    }
};

class User {
private:
    string userName;
    double balance;
    unordered_map<string, int> portfolio;
    vector<Transaction> transactionHistory;

public:
    // Default constructor
    User() : userName(""), balance(0.0) {}

    User(const string& userName, double balance) : userName(userName), balance(balance) {}

    void addBalance(double amount) {
        balance += amount;
    }

    bool subtractBalance(double amount) {
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }

    double getBalance() const {
        return balance;
    }

    void addStock(const string& symbol, int quantity) {
        portfolio[symbol] += quantity;
    }

    bool removeStock(const string& symbol, int quantity) {
        if (portfolio[symbol] < quantity) return false;
        portfolio[symbol] -= quantity;
        if (portfolio[symbol] == 0) portfolio.erase(symbol);
        return true;
    }

    void addTransaction(const Transaction& transaction) {
        transactionHistory.push_back(transaction);
    }

    void printPortfolio() const {
        cout << "Portfolio of " << userName << ":" << endl;
        for (const auto& [symbol, quantity] : portfolio) {
            cout << symbol << ": " << quantity << " shares" << endl;
        }
    }

    void printTransactionHistory() const {
        cout << "Transaction History of " << userName << ":" << endl;
        for (const auto& transaction : transactionHistory) {
            transaction.printTransaction();
        }
    }
};

class Market {
private:
    unordered_map<string, Stock> stocks;

public:
    void addStock(const Stock& stock) {
        stocks[stock.getSymbol()] = stock;
    }

    bool updateStockPrice(const string& symbol, double newPrice) {
        auto it = stocks.find(symbol);
        if (it != stocks.end()) {
            it->second.setPrice(newPrice);
            return true;
        }
        return false;
    }

    double getStockPrice(const string& symbol) const {
        auto it = stocks.find(symbol);
        if (it != stocks.end()) {
            return it->second.getPrice();
        }
        return -1; // Stock not found
    }
};

class StockBrokerageSystem {
private:
    unordered_map<string, User> users;
    Market market;
    mutable mutex mtx;

public:
    void createUser(const string& username, double initialBalance) {
        lock_guard<mutex> lock(mtx);
        users[username] = User(username, initialBalance);
    }

    bool buyStock(const string& username, const string& symbol, int quantity) {
        lock_guard<mutex> lock(mtx);
        auto userIt = users.find(username);
        if (userIt == users.end()) return false;

        double price = market.getStockPrice(symbol);
        if (price < 0) return false;

        double totalCost = price * quantity;
        if (!userIt->second.subtractBalance(totalCost)) return false;

        userIt->second.addStock(symbol, quantity);
        userIt->second.addTransaction(Transaction("Bought", symbol, quantity, price));
        return true;
    }

    bool sellStock(const string& username, const string& symbol, int quantity) {
        lock_guard<mutex> lock(mtx);
        auto userIt = users.find(username);
        if (userIt == users.end()) return false;

        double price = market.getStockPrice(symbol);
        if (price < 0) return false;

        if (!userIt->second.removeStock(symbol, quantity)) return false;

        double totalRevenue = price * quantity;
        userIt->second.addBalance(totalRevenue);
        userIt->second.addTransaction(Transaction("Sold", symbol, quantity, price));
        return true;
    }

    void viewPortfolio(const string& username) const {
        auto userIt = users.find(username);
        if (userIt != users.end()) {
            userIt->second.printPortfolio();
        }
    }

    void viewTransactionHistory(const string& username) const {
        auto userIt = users.find(username);
        if (userIt != users.end()) {
            userIt->second.printTransactionHistory();
        }
    }

    void addStockToMarket(const Stock& stock) {
        lock_guard<mutex> lock(mtx);
        market.addStock(stock);
    }

    bool updateStockPriceInMarket(const string& symbol, double newPrice) {
        lock_guard<mutex> lock(mtx);
        return market.updateStockPrice(symbol, newPrice);
    }
};

int main() {
    StockBrokerageSystem system;
    system.createUser("Alice", 10000.0);
    system.createUser("Bob", 15000.0);

    system.addStockToMarket(Stock("AAPL", 150.0));
    system.addStockToMarket(Stock("GOOGL", 2800.0));
    system.addStockToMarket(Stock("AMZN", 3400.0));

    system.buyStock("Alice", "AAPL", 10);
    system.buyStock("Bob", "GOOGL", 5);

    system.viewPortfolio("Alice");
    system.viewTransactionHistory("Alice");

    system.sellStock("Alice", "AAPL", 5);
    system.viewPortfolio("Alice");
    system.viewTransactionHistory("Alice");

    system.updateStockPriceInMarket("AAPL", 155.0);
    system.buyStock("Alice", "AAPL", 10);
    system.viewPortfolio("Alice");
    system.viewTransactionHistory("Alice");

    return 0;
}
