#include <bits/stdc++.h>
using namespace std;

// ---------- User ----------
class User {
protected:
    string userId;
    string name;

public:
    User(const string& id, const string& name) : userId(id), name(name) {}
    virtual void display() const = 0;
    string getUserId() const { return userId; }
};

// ---------- Buyer ----------
class Buyer : public User {
public:
    Buyer(const string& id, const string& name) : User(id, name) {}
    void display() const override {
        cout << "Buyer: " << name << " (ID: " << userId << ")\n";
    }
};

// ---------- Seller ----------
class Seller : public User {
private:
    double balance = 0;

public:
    Seller(const string& id, const string& name) : User(id, name) {}
    void display() const override {
        cout << "Seller: " << name << " (ID: " << userId << ") | Balance: " << balance << endl;
    }

    void credit(double amount) { balance += amount; }
    void debit(double amount) { balance -= amount; }
    double getBalance() const { return balance; }
};

// ---------- Item ----------
class Item {
private:
    string itemId;
    string name;
    string description;
    double basePrice;
    Seller* seller;

public:
    Item(const string& itemId, const string& name, const string& desc, double price, Seller* seller)
        : itemId(itemId), name(name), description(desc), basePrice(price), seller(seller) {}

    void display() const {
        cout << "Item: " << name << " (ID: " << itemId << ") | Base Price: " << basePrice << endl;
    }

    string getItemId() const { return itemId; }
    Seller* getSeller() const { return seller; }
    double getBasePrice() const { return basePrice; }
};

// ---------- Bid ----------
class Bid {
private:
    Buyer* bidder;
    double amount;

public:
    Bid(Buyer* b, double amt) : bidder(b), amount(amt) {}

    Buyer* getBidder() const { return bidder; }
    double getAmount() const { return amount; }
};

// ---------- Auction ----------
class Auction {
private:
    Item* item;
    vector<Bid> bids;
    bool active = false;

public:
    Auction(Item* item) : item(item) {}

    void startAuction() { active = true; }

    void endAuction() {
        active = false;
        if (bids.empty()) {
            cout << "No bids placed on item: " << item->getItemId() << endl;
            return;
        }

        Bid winning = getWinningBid();
        cout << "Winning bid for item '" << item->getItemId() << "' is " << winning.getAmount()
             << " by buyer " << winning.getBidder()->getUserId() << endl;
    }

    void placeBid(Buyer* buyer, double amount) {
        if (!active) {
            cout << "Auction is not active.\n";
            return;
        }
        if (!bids.empty() && amount <= bids.back().getAmount()) {
            cout << "Bid too low!\n";
            return;
        }
        bids.emplace_back(buyer, amount);
        cout << "Bid placed: " << amount << " by " << buyer->getUserId() << endl;
    }

    void displayBids() const {
        for (auto& b : bids) {
            cout << b.getBidder()->getUserId() << " -> " << b.getAmount() << endl;
        }
    }

    Bid getWinningBid() const {
        return *max_element(bids.begin(), bids.end(),
                            [](const Bid& a, const Bid& b) { return a.getAmount() < b.getAmount(); });
    }

    bool isActive() const { return active; }
    Item* getItem() const { return item; }
};

// ---------- BillingSystem ----------
class BillingSystem {
private:
    double listingFeePercentage = 2.0;
    double sellingFeePercentage = 5.0;
    double platformRevenue = 0;

public:
    void chargeListingFee(Seller* seller, double amount) {
        double fee = (listingFeePercentage / 100.0) * amount;
        seller->debit(fee);
        platformRevenue += fee;
    }

    void chargeSellingFee(Seller* seller, double amount) {
        double fee = (sellingFeePercentage / 100.0) * amount;
        seller->debit(fee);
        platformRevenue += fee;
    }

    double getPlatformRevenue() const { return platformRevenue; }
};

// ---------- EBayPlatform ----------
class EBayPlatform {
private:
    vector<User*> users;
    vector<Item*> items;
    vector<Auction*> auctions;
    BillingSystem billing;
    int itemCounter = 1;

public:
    void registerUser(User* user) {
        users.push_back(user);
    }

    void listItem(Seller* seller, const string& name, const string& desc, double price) {
        string itemId = "item-" + to_string(itemCounter++);
        Item* item = new Item(itemId, name, desc, price, seller);
        items.push_back(item);
        billing.chargeListingFee(seller, price);
        cout << "Item listed: " << itemId << endl;
    }

    void startAuction(const string& itemId) {
        for (auto& item : items) {
            if (item->getItemId() == itemId) {
                Auction* auction = new Auction(item);
                auction->startAuction();
                auctions.push_back(auction);
                cout << "Auction started for item: " << itemId << endl;
                return;
            }
        }
        cout << "Item not found.\n";
    }

    void bidItem(const string& itemId, Buyer* buyer, double amount) {
        for (auto& auction : auctions) {
            if (auction->getItem()->getItemId() == itemId && auction->isActive()) {
                auction->placeBid(buyer, amount);
                return;
            }
        }
        cout << "Auction not found or inactive.\n";
    }

    void endAuction(const string& itemId) {
        for (auto& auction : auctions) {
            if (auction->getItem()->getItemId() == itemId) {
                auction->endAuction();
                Bid winner = auction->getWinningBid();
                Seller* seller = auction->getItem()->getSeller();
                seller->credit(winner.getAmount());
                billing.chargeSellingFee(seller, winner.getAmount());
                return;
            }
        }
        cout << "Auction not found.\n";
    }

    void showRevenue() const {
        cout << "Platform revenue: " << billing.getPlatformRevenue() << endl;
    }
};

// ---------- Main ----------
int main() {
    EBayPlatform platform;

    Seller* s1 = new Seller("S001", "Alice");
    Buyer* b1 = new Buyer("B001", "Bob");
    Buyer* b2 = new Buyer("B002", "Charlie");

    platform.registerUser(s1);
    platform.registerUser(b1);
    platform.registerUser(b2);

    platform.listItem(s1, "iPhone", "Brand new iPhone 15", 70000);
    platform.startAuction("item-1");

    platform.bidItem("item-1", b1, 71000);
    platform.bidItem("item-1", b2, 72000);

    platform.endAuction("item-1");

    platform.showRevenue();

    return 0;
}
