#include<bits/stdc++.h>

using namespace std;

class User {
    string id, name, email;
    
    public:
    User(string id, string name, string email) : id(id) , name(name), email(email) {}
    
    string getId() const {
        return id;
    }
    
    string getName() const {
        return name;
    }
};

class Split {
    protected:
    User* user;
    double amount;
    
    public:
    Split(User* user, double amount) : user(user), amount(amount) {}
    virtual ~Split() {}
    
    User* getUser() const {
        return user;
    }
    
    double getAmount() const {
        return amount;
    }
};

class Expense {
    protected:
    string description;
    double totalAmount;
    User* paidBy;
    vector<Split*> splits;
    
    public:
    Expense(string desc, double total, User* paidBy, vector<Split*> splits) : 
    description(desc), totalAmount(total), paidBy(paidBy), splits(splits) {}
    
    virtual ~Expense() {
        for (auto s : splits) delete s;
    }
    
    vector<Split*> getSplits() const {
        return splits;
    }
    
    User* getPayer() const {
        return paidBy;
    }
    
    double getAmount() const {
        return totalAmount;
    }
    
    string getDescription() const {
        return description;
    }
    
    virtual bool validate() = 0;
};

class EqualExpense : public Expense {
    public:
    EqualExpense(string desc, double total, User* paidBy, vector<User*> participants) : Expense(desc, total, paidBy, {}) {
        double share = total / participants.size();
        for(User* user : participants){
            splits.push_back(new Split(user, share));
        }
    }
    
    bool validate() override{
        return true;
    }
};

class ExactExpense : public Expense {
public:
    ExactExpense(string desc, double total, User* paidBy, vector<Split*> splits)
        : Expense(desc, total, paidBy, splits) {}

    bool validate() override {
        double sum = 0;
        for (auto split : splits) sum += split->getAmount();
        return abs(sum - totalAmount) < 1e-6;
    }
};

class PercentageExpense : public Expense {
public:
    PercentageExpense(string desc, double total, User* paidBy, vector<pair<User*, double>> percentages)
        : Expense(desc, total, paidBy, {}) {
        for (auto& p : percentages) {
            double amt = total * p.second / 100.0;
            splits.push_back(new Split(p.first, amt));
        }
    }

    bool validate() override {
        double sum = 0;
        for (auto split : splits) sum += split->getAmount();
        return abs(sum - totalAmount) < 1e-6;
    }
};

class Group {
    string groupId, groupName;
    unordered_set<User*> members;
    vector<Expense*> expenses;

public:
    Group(string id, string name) : groupId(id), groupName(name) {}

    void addMember(User* user) {
        members.insert(user);
    }

    void addExpense(Expense* expense) {
        if (expense->validate()) {
            expenses.push_back(expense);
        } else {
            cout << "Invalid expense: " << expense->getDescription() << "\n";
        }
    }

    const vector<Expense*>& getExpenses() const {
        return expenses;
    }

    const unordered_set<User*>& getMembers() const {
        return members;
    }
};

class ExpenseManager {
    unordered_map<string, User*> users;
    unordered_map<string, Group*> groups;
    unordered_map<string, unordered_map<string, double>> balanceSheet;

public:
    ~ExpenseManager() {
        for (auto& p : users) delete p.second;
        for (auto& g : groups) delete g.second;
    }

    User* createUser(string id, string name, string email) {
        users[id] = new User(id, name, email);
        return users[id];
    }

    Group* createGroup(string id, string name) {
        groups[id] = new Group(id, name);
        return groups[id];
    }

    void addUserToGroup(string groupId, string userId) {
        if (groups.count(groupId) && users.count(userId)) {
            groups[groupId]->addMember(users[userId]);
        }
    }

    void addExpense(string groupId, Expense* expense) {
        if (!groups.count(groupId)) return;

        groups[groupId]->addExpense(expense);
        User* paidBy = expense->getPayer();
        for (auto split : expense->getSplits()) {
            string paidUserId = paidBy->getId();
            string oweUserId = split->getUser()->getId();
            if (paidUserId == oweUserId) continue;

            balanceSheet[oweUserId][paidUserId] += split->getAmount();
            balanceSheet[paidUserId][oweUserId] -= split->getAmount();
        }
    }

    void showBalances() {
        for (auto& from : balanceSheet) {
            for (auto& to : from.second) {
                if (to.second > 0.0) {
                    cout << users[from.first]->getName() << " owes "
                         << users[to.first]->getName() << ": ₹" << to.second << "\n";
                }
            }
        }
    }

    void showUserBalance(string userId) {
        if (!users.count(userId)) return;
        for (auto& to : balanceSheet[userId]) {
            if (to.second > 0.0) {
                cout << users[userId]->getName() << " owes "
                     << users[to.first]->getName() << ": ₹" << to.second << "\n";
            }
        }
    }
};

int main() {
    ExpenseManager manager;

    // Create users
    User* u1 = manager.createUser("u1", "Alice", "alice@email.com");
    User* u2 = manager.createUser("u2", "Bob", "bob@email.com");
    User* u3 = manager.createUser("u3", "Charlie", "charlie@email.com");

    // Create group
    Group* trip = manager.createGroup("g1", "Goa Trip");
    manager.addUserToGroup("g1", "u1");
    manager.addUserToGroup("g1", "u2");
    manager.addUserToGroup("g1", "u3");

    // Equal expense ₹300 paid by Alice
    manager.addExpense("g1", new EqualExpense("Dinner", 300, u1, {u1, u2, u3}));

    // Exact expense ₹300: Bob paid, Charlie owes 200, Alice owes 100
    manager.addExpense("g1", new ExactExpense("Cab", 300, u2,
        { new Split(u1, 100), new Split(u3, 200) }));

    // Percentage expense ₹1000: Charlie paid, Alice 50%, Bob 30%, Charlie 20%
    manager.addExpense("g1", new PercentageExpense("Hotel", 1000, u3,
        { {u1, 50}, {u2, 30}, {u3, 20} }));

    // Show balances
    manager.showBalances();
    manager.showUserBalance("u1");

    return 0;
}


