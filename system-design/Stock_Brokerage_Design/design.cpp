#include<bits/stdc++.h>

using namespace std;

enum class OrderType {
    BUY,
    SELL
};

struct Stock {
    std::string symbol;
    double price; // Simulated real-time price

    Stock(std::string symbol, double price) : symbol(symbol), price(price) {}
};

struct Transaction {
    std::string stockSymbol;
    int quantity;
    double price;
    OrderType type;
    std::time_t timestamp;

    Transaction(std::string s, int q, double p, OrderType t)
        : stockSymbol(s), quantity(q), price(p), type(t), timestamp(std::time(nullptr)) {}
};

class Portfolio {
    unordered_map<string,int> holdings;
    
    public:
    void addStock(string symbol, int quantity) {
        holdings[symbol] += quantity;
    }
    
    bool removeStock(string symbol, int quantity) {
        if(holdings[symbol] < quantity) return false;
        holdings[symbol] -= quantity;
        if(holdings[symbol] == 0) holdings.erase(symbol);
        return true;
    }
    
    void viewPortfolio() {
        std::cout << "\n--- Portfolio ---\n";
        for (auto& [symbol, qty] : holdings) {
            std::cout << symbol << ": " << qty << " shares\n";
        }
        std::cout << "------------------\n";
    }

    
    int getStockQuantity(const string& symbol) {
        return holdings.count(symbol) ? holdings[symbol] : 0;
    }
};

class Account {
    std::string username;
    double balance;
    Portfolio portfolio;
    std::vector<Transaction> history;

public:
    Account(std::string uname, double initBalance) : username(uname), balance(initBalance) {}

    std::string getUsername() const { return username; }

    double getBalance() const { return balance; }

    void deposit(double amount) {
        balance += amount;
    }

    void withdraw(double amount) {
        if (amount > balance) throw std::runtime_error("Insufficient funds.");
        balance -= amount;
    }

    Portfolio& getPortfolio() { return portfolio; }

    void addTransaction(const Transaction& tx) {
        history.push_back(tx);
    }

    void viewTransactionHistory() {
        std::cout << "\n--- Transaction History for " << username << " ---\n";
        for (auto& tx : history) {
            std::cout << (tx.type == OrderType::BUY ? "BUY" : "SELL")
                      << " " << tx.quantity << " " << tx.stockSymbol
                      << " @ " << tx.price << " on " << std::ctime(&tx.timestamp);
        }
        std::cout << "----------------------------------------------\n";
    }
};

class Market {
    std::unordered_map<std::string, std::shared_ptr<Stock>> stocks;

public:
    Market() {
        // Pre-populate some stocks
        stocks["AAPL"] = std::make_shared<Stock>("AAPL", 150.00);
        stocks["GOOG"] = std::make_shared<Stock>("GOOG", 2800.00);
        stocks["TSLA"] = std::make_shared<Stock>("TSLA", 720.00);
    }

    std::shared_ptr<Stock> getStock(const std::string& symbol) {
        return stocks.count(symbol) ? stocks[symbol] : nullptr;
    }

    void simulatePriceFluctuation() {
        for (auto& [_, stock] : stocks) {
            double change = ((rand() % 100) - 50) / 100.0;
            stock->price = std::max(1.0, stock->price + change);
        }
    }

    void showMarketData() {
        std::cout << "\n--- Market Prices ---\n";
        for (auto& [_, stock] : stocks) {
            std::cout << stock->symbol << ": $" << std::fixed << std::setprecision(2) << stock->price << "\n";
        }
        std::cout << "----------------------\n";
    }
};

class BrokerageSystem {
    std::unordered_map<std::string, std::shared_ptr<Account>> users;
    Market market;

public:
    void createAccount(const std::string& username, double balance) {
        if (users.count(username)) {
            std::cout << "Username already exists!\n";
            return;
        }
        users[username] = std::make_shared<Account>(username, balance);
        std::cout << "Account created for " << username << " with balance $" << balance << "\n";
    }

    std::shared_ptr<Account> getAccount(const std::string& username) {
        return users.count(username) ? users[username] : nullptr;
    }

    void buyStock(const std::string& username, const std::string& symbol, int qty) {
        auto user = getAccount(username);
        if (!user) return;

        auto stock = market.getStock(symbol);
        if (!stock) {
            std::cout << "Invalid stock symbol\n";
            return;
        }

        double cost = stock->price * qty;
        if (user->getBalance() < cost) {
            std::cout << "Insufficient balance to buy\n";
            return;
        }

        user->withdraw(cost);
        user->getPortfolio().addStock(symbol, qty);
        user->addTransaction(Transaction(symbol, qty, stock->price, OrderType::BUY));

        std::cout << "Bought " << qty << " of " << symbol << " @ $" << stock->price << "\n";
    }

    void sellStock(const std::string& username, const std::string& symbol, int qty) {
        auto user = getAccount(username);
        if (!user) return;

        auto stock = market.getStock(symbol);
        if (!stock) {
            std::cout << "Invalid stock symbol\n";
            return;
        }

        if (user->getPortfolio().getStockQuantity(symbol) < qty) {
            std::cout << "Not enough shares to sell\n";
            return;
        }

        user->getPortfolio().removeStock(symbol, qty);
        double revenue = stock->price * qty;
        user->deposit(revenue);
        user->addTransaction(Transaction(symbol, qty, stock->price, OrderType::SELL));

        std::cout << "Sold " << qty << " of " << symbol << " @ $" << stock->price << "\n";
    }

    void viewPortfolio(const std::string& username) {
        auto user = getAccount(username);
        if (user) {
            std::cout << "Balance: $" << user->getBalance() << "\n";
            user->getPortfolio().viewPortfolio();
        }
    }

    void viewTransactions(const std::string& username) {
        auto user = getAccount(username);
        if (user) {
            user->viewTransactionHistory();
        }
    }

    void showMarket() {
        market.simulatePriceFluctuation(); // simulate random change
        market.showMarketData();
    }
};

int main() {
    BrokerageSystem system;

    system.createAccount("alice", 10000);
    system.createAccount("bob", 5000);

    system.showMarket();

    system.buyStock("alice", "AAPL", 10);
    system.sellStock("alice", "AAPL", 5);

    system.viewPortfolio("alice");
    system.viewTransactions("alice");

    system.showMarket();

    system.buyStock("bob", "TSLA", 5);
    system.viewPortfolio("bob");

    return 0;
}


