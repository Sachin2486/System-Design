#include <bits/stdc++.h>
using namespace std;

class User
{
private:
    int user_id;
    string user_name;
    double balance;

public:
    User(int user_id, const string &user_name);

    int getId() const;
    string getName() const;
    double getBalance() const;

    void updateBalance(double amount);
};

class SplitExpense
{
public:
    virtual void calculateAmounts(double totalAmount, int numParticipants) = 0;
};

class SplitExpenseEqually : public SplitExpense
{
public:
    void calculateAmounts(double totalAmount, int numParticipants) override;
};

class SplitExpenseInPercentage : public SplitExpense
{
private:
    vector<double> percentages;

public:
    SplitExpenseInPercentage(const vector<double> &percentages);
    void calculateAmounts(double totalAmount, int numParticipants) override;
};

class SplitExpenseExactly : public SplitExpense
{
private:
    vector<double> amounts;

public:
    SplitExpenseExactly(const vector<double> &amounts);

    void calculateAmounts(double totalAmount, int numParticipants) override;
};

class Expense
{
private:
    double amount;
    string description;
    int payerID;
    SplitExpense *splitExpense;

public:
    Expense(double amount, const string &description, int payerID, SplitExpense *splitExpense);
    ~Expense();

    double getAmount() const;
    string getDescription() const;
    int getPayerID() const;
    SplitExpense *getSplitExpense() const;
};

class ExpenseManager
{
private:
    vector<User> users;
    vector<Expense> expenses;

public:
    void addUser(const User &user);
    void addExpense(const Expense &expense);
    void calculateUserBalances();
    void showUserBalances();
    void settleExpenses();
};
