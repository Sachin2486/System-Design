#include<bits/stdc++.h>

using namespace std;

class Card {
public:
	string cardNumber;
	string pin;

	Card(string num, string pinCode) : cardNumber(num), pin(pinCode) {}

};

class Account {
private:
	string cardNumber;
	double balance;
	mutex mtx;

public:
	Account(string cardNum, double bal) : cardNumber(cardNum), balance(bal) {}

	bool withdraw(double amount) {
		lock_guard<mutex> lock(mtx);
		if(balance >= amount) {
			balance -= amount;
			return true;
		}
		return false;
	}

	void deposit(double amount) {
		lock_guard<mutex> lock(mtx);
		balance += amount;
	}

	double getBalance() {
		lock_guard<mutex> lock(mtx);
		return balance;
	}
};

class BankSystem {
private:
	unordered_map<string,string> pinDB;
	unordered_map<string, Account*> accounts;

public:
	BankSystem() {
		pinDB["1234"] = "0000";
		accounts["1234"] = new Account("1234", 10000.0);
	}

	bool validateCard(string cardNumber, string pin) {
		return pinDB.count(cardNumber) && pinDB[cardNumber] == pin;
	}

	Account* getAccount(string cardNumber) {
		if (accounts.count(cardNumber)) return accounts[cardNumber];
        return nullptr;
	}

	~BankSystem() {
		for (auto& kv : accounts) delete kv.second;
	}
};

class CashDispenser {
private:
	double availableCash;
	mutex mtx;

public:
	CashDispenser(double cash) : availableCash(cash) {}

	bool dispense(double amount) {
		lock_guard<mutex> lock(mtx);
		if (availableCash >= amount) {
			availableCash -= amount;
			return true;
		}
		return false;
	}

	void refill(double amount) {
		lock_guard<mutex> lock(mtx);
		availableCash += amount;
	}

	double getAvailableCash() {
		lock_guard<mutex> lock(mtx);
		return availableCash;
	}
};

class ATM {
private:
	BankSystem& bank;
	CashDispenser& dispenser;
	string currentCard;
	Account* currentAccount;

public:
	ATM(BankSystem& b, CashDispenser& d) : bank(b), dispenser(d), currentAccount(nullptr) {}

	bool authenticateUser(const Card& card) {
		if (bank.validateCard(card.cardNumber, card.pin)) {
			currentCard = card.cardNumber;
			currentAccount = bank.getAccount(currentCard);
			return true;
		}
		return false;
	}

	void showMenu() {
		int choice;
		do {
			cout << "\n--- ATM MENU ---\n";
			cout << "1. Balance Inquiry\n";
			cout << "2. Deposit Cash\n";
			cout << "3. Withdraw Cash\n";
			cout << "4. Exit\n";
			cout << "Enter choice: ";
			cin >> choice;

			switch (choice) {
			case 1:
				handleBalanceInquiry();
				break;
			case 2:
				handleDeposit();
				break;
			case 3:
				handleWithdrawal();
				break;
			case 4:
				cout << "Thank you for using the ATM.\n";
				break;
			default:
				cout << "Invalid option.\n";
			}
		} while (choice != 4);
	}

private:
	void handleBalanceInquiry() {
		cout << "Current Balance: b9" << currentAccount->getBalance() << endl;
	}

	void handleDeposit() {
		double amount;
		cout << "Enter amount to deposit: b9";
		cin >> amount;
		currentAccount->deposit(amount);
		cout << "Deposited successfully.\n";
	}

	void handleWithdrawal() {
		double amount;
		cout << "Enter amount to withdraw: b9";
		cin >> amount;

		if (!dispenser.dispense(amount)) {
			cout << "ATM has insufficient cash.\n";
			return;
		}

		if (currentAccount->withdraw(amount)) {
			cout << "Please collect your cash.\n";
		} else {
			cout << "Insufficient account balance.\n";
			dispenser.refill(amount); // rollback cash
		}
	}
};

int main() {
	BankSystem bank;
	CashDispenser dispenser(50000.0);
	ATM atm(bank, dispenser);

	string cardNum, pin;
	cout << "Insert card (enter card number): ";
	cin >> cardNum;
	cout << "Enter PIN: ";
	cin >> pin;

	Card card(cardNum, pin);
	if (atm.authenticateUser(card)) {
		atm.showMenu();
	} else {
		cout << "Authentication failed. Invalid card or PIN.\n";
	}

	return 0;
}