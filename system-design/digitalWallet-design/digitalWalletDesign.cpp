#include<bits/stdc++.h>

using namespace std;

class PaymentMethod {
public:
	virtual string getType() const = 0;
	virtual string getDetails() const = 0;
	virtual ~PaymentMethod() = default;
};

class CreditCard : public PaymentMethod {
	string cardNumber;

public:
	CreditCard(const string& number) : cardNumber(number) {}
	string getType() const override {
		return "Credit Card";
	}
	string getDetails() const override {
		return "Card Number:" + cardNumber;
	}
};

class BankAccount : public PaymentMethod {
	std :: string accountNumber;
public:
	BankAccount(const string& number) : accountNumber(number) {}
	string getType() const override {
		return "Bank Accuont";
	}
	string getDetails() const override {
		return "Account Number: " + accountNumber;
	}
};

struct Transaction {
	string type;
	double amount;
	string currency;
	string date;
	string details;

	void print() const {
		std::cout << date << " | " << type << " | " << std::fixed << std::setprecision(2) << amount
		          << " " << currency << " | " << details << "\n";
	}
};

class Wallet {
	double balance;
	std::string currency;
	std::vector<std::unique_ptr<PaymentMethod>> paymentMethods;
	std::vector<Transaction> transactionHistory;

public:
	Wallet(const std::string& currency = "USD") : balance(0.0), currency(currency) {}

	void addPaymentMethod(std::unique_ptr<PaymentMethod> method) {
		paymentMethods.push_back(std::move(method));
	}

	void removePaymentMethod(const std::string& type) {
		paymentMethods.erase(
		    std::remove_if(paymentMethods.begin(), paymentMethods.end(),
		[&type](const std::unique_ptr<PaymentMethod>& method) {
			return method->getType() == type;
		}),
		paymentMethods.end());
	}

	void addFunds(double amount, const std::string& currencyType) {
		balance += convertCurrency(amount, currencyType, currency);
		transactionHistory.push_back({"Deposit", amount, currencyType, getCurrentDate(), "Added funds"});
	}

	bool transferFunds(double amount, Wallet& recipient, const std::string& currencyType) {
		double convertedAmount = convertCurrency(amount, currencyType, currency);
		if (balance >= convertedAmount) {
			balance -= convertedAmount;
			recipient.addFunds(amount, currencyType);
			transactionHistory.push_back({"Transfer", amount, currencyType, getCurrentDate(), "Transfer to user"});
			return true;
		}
		return false;
	}

	void showTransactionHistory() const {
		std::cout << "Transaction History:\n";
		for (const auto& transaction : transactionHistory) {
			transaction.print();
		}
	}

	void showPaymentMethods() const {
		std::cout << "Payment Methods:\n";
		for (const auto& method : paymentMethods) {
			std::cout << "- " << method->getType() << ": " << method->getDetails() << "\n";
		}
	}

private:
	double convertCurrency(double amount, const std::string& fromCurrency, const std::string& toCurrency) const {
		static std::map<std::pair<std::string, std::string>, double> exchangeRates = {
			{{"USD", "EUR"}, 0.85}, {{"EUR", "USD"}, 1.18},
			{{"USD", "INR"}, 74.0}, {{"INR", "USD"}, 0.0135}
		};
		if (fromCurrency == toCurrency) return amount;
		return amount * exchangeRates[ {fromCurrency, toCurrency}];
	}

	std::string getCurrentDate() const {
		return "2024-11-01"; 
	}
};

class User {
	std::string name;
	std::string email;
	Wallet wallet;
public:
	User(const std::string& name, const std::string& email, const std::string& currency)
		: name(name), email(email), wallet(currency) {}

	void updatePersonalInfo(const std::string& newName, const std::string& newEmail) {
		name = newName;
		email = newEmail;
	}

	Wallet& getWallet() {
		return wallet;
	}

	void displayInfo() const {
		std::cout << "User: " << name << " (" << email << ")\n";
	}
};


int main() {
	User user1("Alice", "alice@example.com", "USD");
	user1.getWallet().addPaymentMethod(std::make_unique<CreditCard>("1234-5678-9012-3456"));
	user1.getWallet().addFunds(100, "USD");

	User user2("Bob", "bob@example.com", "USD");
	user2.getWallet().addPaymentMethod(std::make_unique<BankAccount>("9876543210"));

	user1.displayInfo();
	user1.getWallet().showPaymentMethods();
	user1.getWallet().showTransactionHistory();

	std::cout << "\nTransferring $50 from Alice to Bob:\n";
	if (user1.getWallet().transferFunds(50, user2.getWallet(), "USD")) {
		std::cout << "Transfer successful!\n";
	} else {
		std::cout << "Insufficient balance.\n";
	}

	std::cout << "\nBob's transaction history after receiving funds:\n";
	user2.getWallet().showTransactionHistory();

	return 0;
}