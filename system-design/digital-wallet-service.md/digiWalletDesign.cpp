#include <bits/stdc++.h>
#include <iostream>

using namespace std;

enum class Currency { USD, INR, EUR };

string currencyToStr(Currency c) {
	switch (c) {
	case Currency::USD:
		return "USD";
	case Currency::INR:
		return "INR";
	case Currency::EUR:
		return "EUR";
	default:
		return "UNKNOWN";
	}
}

// Transaction Model
struct Transaction {
	string id;
	double amount;
	Currency currency;
	string fromUserId;
	string toUserId;
	time_t timestamp;

	Transaction(string id, double amt, Currency cur, string from, string to)
		: id(id), amount(amt), currency(cur), fromUserId(from), toUserId(to) {
		timestamp = time(nullptr);
	}
};

class PaymentMethod {
public:
	virtual string getDetails() const = 0;
	virtual ~PaymentMethod() = default;
};

class CreditCard : public PaymentMethod {
	string cardNumber;
public:
	CreditCard(const string& num) : cardNumber(num) {}
	string getDetails() const override {
		return "CreditCard: ****" + cardNumber.substr(cardNumber.size() - 4);
	}
};

class BankAccount : public PaymentMethod {
	string accountNumber;
public:
	BankAccount(const string& num) : accountNumber(num) {}
	string getDetails() const override {
		return "BankAccount: ****" + accountNumber.substr(accountNumber.size() - 4);
	}
};

class CurrencyConverter {
public:
	static double getRate(Currency from, Currency to) {
		if (from == to) return 1.0;
		if (from == Currency::USD && to == Currency::INR) return 83.0;
		if (from == Currency::INR && to == Currency::USD) return 0.012;
		if (from == Currency::EUR && to == Currency::USD) return 1.1;
		// Extend this as needed
		return 1.0;
	}
};

class Wallet {
private:
	unordered_map<Currency, double> balances;
	vector<shared_ptr<PaymentMethod>> methods;
	vector <Transaction> transactions;
	mutex mtx;

public:
	void addPaymentMethod(shared_ptr<PaymentMethod> method) {
		lock_guard<mutex> lock(mtx);
		methods.push_back(method);
	}

	void addFunds(double amount, Currency currency) {
		lock_guard<mutex> lock(mtx);
		balances[currency] += amount;
	}

	bool deductFunds(double amount, Currency currency) {
		lock_guard<mutex> lock(mtx);
		if(balances[currency] >= amount) {
			balances[currency] -= amount;
			return true;
		}
		return false;
	}

	void addTransaction(const Transaction& txn) {
		lock_guard<mutex> lock(mtx);
		transactions.push_back(txn);
	}

	void printStatement() {
		lock_guard<mutex> lock(mtx);
		cout << "Transaction History:\n";
		for (auto& t : transactions) {
			cout << "TxnID: " << t.id << " | Amount: " << t.amount << " " << currencyToStr(t.currency)
			     << " | From: " << t.fromUserId << " | To: " << t.toUserId << " | Time: " << ctime(&t.timestamp);
		}
	}

	void showBalance() {
		lock_guard<mutex> lock(mtx);
		cout << "Wallet Balances:\n";
		for (auto& [currency, amount] : balances) {
			cout << currencyToStr(currency) << ": " << amount << endl;
		}
	}

};

class User {
public:
	string userId;
	string name;
	string email;
	Wallet wallet;

	User(string id, string name, string email)
		: userId(id), name(name), email(email) {}
};

class WalletService {
private:
	unordered_map<string, shared_ptr<User>> users;
	mutex mtx;

	string generateTxnId() {
		static int counter = 1;
		return "TXN" + to_string(counter++);
	}

public:
	void createUser(const string& userId, const string& name, const string&email) {
		lock_guard<mutex> lock(mtx);
		users[userId] = make_shared<User>(userId,name, email);
	}

	void addPaymentMethod(const string& userId, shared_ptr<PaymentMethod> method) {
		if (users.count(userId))
			users[userId]->wallet.addPaymentMethod(method);
	}

	void fundWallet(const string& userId, double amount, Currency currency) {
		if (users.count(userId))
			users[userId]->wallet.addFunds(amount, currency);
	}

	bool transferFunds(const string& fromUser, const string& toUser, double amount, Currency currency) {
		if (!users.count(fromUser) || !users.count(toUser)) return false;

		double rate = CurrencyConverter::getRate(currency, currency);
		if (users[fromUser]->wallet.deductFunds(amount, currency)) {
			double convertedAmount = amount * rate;
			users[toUser]->wallet.addFunds(convertedAmount, currency);

			Transaction txn(generateTxnId(), amount, currency, fromUser, toUser);
			users[fromUser]->wallet.addTransaction(txn);
			users[toUser]->wallet.addTransaction(txn);
			return true;
		}

		return false;
	};

	void showUserBalance(const string& userId) {
		if (users.count(userId))
			users[userId]->wallet.showBalance();
	}

	void printUserStatement(const string& userId) {
		if (users.count(userId))
			users[userId]->wallet.printStatement();
	}
};

int main() {
	WalletService service;

	service.createUser("u1", "Sachin", "sachin@example.com");
	service.createUser("u2", "Rahul", "rahul@example.com");

	service.addPaymentMethod("u1", make_shared<CreditCard>("1234567890123456"));
	service.addPaymentMethod("u2", make_shared<BankAccount>("000123456789"));

	service.fundWallet("u1", 100, Currency::USD);
	service.fundWallet("u2", 5000, Currency::INR);

	service.transferFunds("u1", "u2", 50, Currency::USD);

	service.showUserBalance("u1");
	service.showUserBalance("u2");

	service.printUserStatement("u1");
	service.printUserStatement("u2");

	return 0;
}