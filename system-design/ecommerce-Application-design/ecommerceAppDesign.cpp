#include <iostream>
#include <vector>
#include <string>
#include <unordered_map>

using namespace std;

class Product {
private:
	string name;
	string description;
	double price;
	int stock;

public:

	Product() : name(""), description(""), price(0.0), stock(0) {}  // default constructor

	Product(string name, string description, double price, int stock)
		: name(name), description(description), price(price), stock(stock) {}

	string getName() const {
		return name;
	}
	string getDescription() const {
		return description;
	}
	double getPrice() const {
		return price;
	}
	int getStock() const {
		return stock;
	}

	void setStock(int stock) {
		this->stock = stock;
	}
};

class Category {
private:
	string name;
	vector<Product> products;

public:
	Category(string name) : name(name) {}

	void addProduct(Product &product) {
		products.push_back(product);
	}

	Product* searchProduct(string productName) {
		for (auto &product : products) {
			if (product.getName() == productName)
				return &product;
		}
		return nullptr;
	}
};

class User {
private:
	string userName;
	string email;
	vector<Product> cart;
	vector<string> orderHistory;

public:
	User(string userName, string email) : userName(userName), email(email) {}

	void addToCart(Product &product) {
		cart.push_back(product);
	}

	void viewCart() {
		cout << "Shopping Cart:\n";
		for (auto &product : cart) {
			cout << product.getName() << " - $" << product.getPrice() << "\n";
		}
	}

	void addOrderHistory(string productName) {
		orderHistory.push_back(productName);
	}

	void viewOrderHistory() {
		cout << "Order History:\n";
		for (auto &order : orderHistory) {
			cout << order << "\n";
		}
	}
};

class Inventory {
private:
	unordered_map<string, Product*> products;

public:
	void addProduct(Product* p) {
		products[p->getName()] = p;
	}

	bool isAvailable(const string& productName) const {
		auto it = products.find(productName);
		return it != products.end() && it->second->getStock() > 0;
	}

	void updateStock(const string& productName, int quantity) {
		auto it = products.find(productName);
		if (it != products.end()) {
			int newStock = it->second->getStock() - quantity;
			it->second->setStock(newStock);
		}
	}

	Product* getProduct(const string& productName) {
		auto it = products.find(productName);
		return (it != products.end()) ? it->second : nullptr;
	}

	~Inventory() {
		for (auto& pair : products) {
			delete pair.second;
		}
	}
};

class Order {
private:
	vector<Product> products;
	double totalAmount;

public:
	Order(vector<Product> products) : products(products), totalAmount(0) {
		for (auto &product : products) {
			totalAmount += product.getPrice();
		}
	}

	void placeOrder(User &user, Inventory &inventory) {
		for (auto &product : products) {
			inventory.updateStock(product.getName(), 1);
			user.addOrderHistory(product.getName());
		}
		cout << "Order placed successfully! Total amount: $" << totalAmount << "\n";
	}

	double getTotalAmount() const {
		return totalAmount;
	}
};

class Payment {
public:
	static void processPayment(string method, double amount) {
		cout << "Processing " << method << " payment for $" << amount << "\n";
		cout << "Payment successful!\n";
	}
};

class OrderHistory {
public:
	static void viewOrderHistory(User &user) {
		user.viewOrderHistory();
	}
};

int main() {
	Product* laptop = new Product("Laptop", "A powerful laptop", 1200.00, 10);
	Product* phone = new Product("Smartphone", "A high-end smartphone", 800.00, 20);

	// Create category and add products
	Category electronics("Electronics");
	electronics.addProduct(*laptop);
	electronics.addProduct(*phone);

	// Create example dummy user
	User user("JohnDoe", "john@example.com");

	user.addToCart(*laptop);
	user.addToCart(*phone);

	user.viewCart();

	Inventory inventory;
	inventory.addProduct(laptop);
	inventory.addProduct(phone);

	vector<Product> cartItems = {*laptop, *phone};
	Order order(cartItems);
	order.placeOrder(user, inventory);

	Payment::processPayment("Credit Card", order.getTotalAmount());

	OrderHistory::viewOrderHistory(user);

	// Clean up dynamically allocated products
	delete laptop;
	delete phone;

	return 0;
}
