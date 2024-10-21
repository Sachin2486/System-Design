#include<bits/stdc++.h>

using namespace std;

class User {
public:
    string userName;
    string email;
    string userId;

    User(string userName, string email, string userId)
        : userName(userName), email(email), userId(userId) {}

    void updateProfile(string newName, string newEmail) {
        userName = newName;
        email = newEmail;
    }

    void displayProfile() {
        cout << "User: " << userName << " (" << email << ")" << endl;
    }
};

class Group {
    string groupId;
    string groupName;
    std::vector<User*> members;

public:
    Group(string groupId, string groupName) : groupId(groupId), groupName(groupName) {}

    void addUser(User* user) {
        members.push_back(user);
    }

    void displayMembers() {
        cout << "Group: " << groupName << endl;
        for (User* member : members) {
            member->displayProfile();
        }
    }
};

class Expense {
public:
    string description;
    double amount;
    unordered_map<User*, double> shares;

    Expense(string description, double amount) : description(description), amount(amount) {}

    void splitExpense(vector<User*> participants) {
        double splitAmount = amount / participants.size();
        for (User* user : participants) {
            shares[user] = splitAmount;
        }
    }

    void displayExpense() {
        cout << "Expense: " << description << " - $" << amount << endl;
        for (auto& share : shares) {
            cout << share.first->userName << " owes $" << share.second << endl;
        }
    }
};

class ExpenseManager {
    unordered_map<string, unordered_map<string, double>> balanceSheet;

public:
    // Record the balance between two users
    void recordExpense(User* payer, User* payee, double amount) {
        balanceSheet[payer->userId][payee->userId] += amount;
        balanceSheet[payee->userId][payer->userId] -= amount;
    }

    // Settle the balances between two users
    void settleUp(User* user1, User* user2) {
        double balance = balanceSheet[user1->userId][user2->userId];
        balanceSheet[user1->userId][user2->userId] = 0;
        balanceSheet[user2->userId][user1->userId] = 0;

        cout << "Balance settled between " << user1->userName << " and " << user2->userName << endl;
    }

    // Display individual balances with other users
    void showBalances(User* user) {
        cout << "Balances for " << user->userName << ":" << endl;
        for (auto& entry : balanceSheet[user->userId]) {
            if (entry.second != 0) {
                cout << "Owes " << entry.first << ": $" << entry.second << endl;
            }
        }
    }

    // Add an expense and split among participants
    void addExpense(User* payer, string description, double amount, vector<User*> participants) {
        Expense newExpense(description, amount);
        newExpense.splitExpense(participants);

        for (auto& participant : participants) {
            if (participant != payer) {
                recordExpense(participant, payer, newExpense.shares[participant]);
            }
        }

        newExpense.displayExpense();
    }
};

int main() {
    User user1("Alice", "alice@example.com", "u1");
    User user2("Bob", "bob@example.com", "u2");
    User user3("Charlie", "charlie@example.com", "u3");

    Group group1("g1", "Friends");
    group1.addUser(&user1);
    group1.addUser(&user2);
    group1.addUser(&user3);

    group1.displayMembers();

    ExpenseManager manager;

    manager.addExpense(&user1, "Lunch", 60.0, {&user1, &user2, &user3});  // Split equally between Alice, Bob, and Charlie
    manager.addExpense(&user2, "Movie", 30.0, {&user1, &user2});  // Split only between Alice and Bob

    manager.showBalances(&user1);
    manager.showBalances(&user2);
    manager.showBalances(&user3);

    manager.settleUp(&user1, &user2);

    manager.showBalances(&user1);
    manager.showBalances(&user2);

    return 0;
}
