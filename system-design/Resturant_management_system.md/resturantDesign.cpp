
#include <iostream>
#include <vector>
#include <string>
#include <map>
using namespace std;

class MenuItem {
public:
    string name;
    double price;
    string category;
    map<string, int> ingredientsRequired; // ingredient name -> quantity

    MenuItem(string name, double price, string category, map<string, int> ingredients = {})
        : name(name), price(price), category(category), ingredientsRequired(ingredients) {}
};

class Menu {
    vector<MenuItem> items;

public:
    void addItem(const MenuItem &item) {
        items.push_back(item);
    }

    void displayMenu() const {
        cout << "\n------ MENU ------\n";
        for (const auto& item : items) {
            cout << item.name << " (" << item.category << ") - Rs. " << item.price << endl;
        }
    }

    MenuItem* findItem(const string& name) {
        for (auto& item : items) {
            if (item.name == name)
                return &item;
        }
        return nullptr;
    }
};

class Inventory {
    map<string, int> stock; // ingredient -> quantity

public:
    void addStock(const string& ingredient, int quantity) {
        stock[ingredient] += quantity;
    }

    bool hasIngredients(const map<string, int>& needed) {
        for (const auto& [ingredient, qty] : needed) {
            if (stock[ingredient] < qty) return false;
        }
        return true;
    }

    void useIngredients(const map<string, int>& used) {
        for (const auto& [ingredient, qty] : used) {
            stock[ingredient] -= qty;
        }
    }

    void showStock() const {
        cout << "\n--- Inventory Stock ---\n";
        for (const auto& [ingredient, qty] : stock) {
            cout << ingredient << ": " << qty << " units\n";
        }
    }
};

class Order {
private:
    static int orderCounter;
    int orderId;
    vector<MenuItem> orderedItems;
    double totalAmount;

public:
    Order() : orderId(++orderCounter), totalAmount(0) {}

    bool addItem(MenuItem* item, Inventory& inventory) {
        if (!item) {
            cout << "Item not found in menu.\n";
            return false;
        }
        if (!inventory.hasIngredients(item->ingredientsRequired)) {
            cout << "Insufficient ingredients for " << item->name << ".\n";
            return false;
        }

        orderedItems.push_back(*item);
        totalAmount += item->price;
        inventory.useIngredients(item->ingredientsRequired);
        cout << item->name << " added to order.\n";
        return true;
    }

    void showOrder() const {
        cout << "\nOrder ID: " << orderId << "\nItems Ordered:\n";
        for (const auto& item : orderedItems) {
            cout << "- " << item.name << " - Rs. " << item.price << endl;
        }
        cout << "Total Amount: Rs. " << totalAmount << endl;
    }
};

int Order::orderCounter = 0;

class RestaurantManagementSystem {
private:
    Menu menu;
    Inventory inventory;

public:
    void setupMenu() {
        menu.addItem(MenuItem("Pizza", 250.0, "Main Course", { {"Dough", 1}, {"Cheese", 2}, {"Tomato", 1} }));
        menu.addItem(MenuItem("Pasta", 200.0, "Main Course", { {"Pasta", 1}, {"Cheese", 1}, {"Sauce", 1} }));
        menu.addItem(MenuItem("Coke", 50.0, "Beverage", { {"CokeBottle", 1} }));
        menu.addItem(MenuItem("Brownie", 120.0, "Dessert", { {"Chocolate", 2}, {"Flour", 1} }));
    }

    void setupInventory() {
        inventory.addStock("Dough", 5);
        inventory.addStock("Cheese", 10);
        inventory.addStock("Tomato", 5);
        inventory.addStock("Pasta", 5);
        inventory.addStock("Sauce", 5);
        inventory.addStock("CokeBottle", 5);
        inventory.addStock("Chocolate", 5);
        inventory.addStock("Flour", 5);
    }

    void takeOrder() {
        Order order;
        string choice;
        while (true) {
            cout << "\nEnter item name to add (or type 'done' to finish): ";
            getline(cin, choice);
            if (choice == "done") break;
            MenuItem* item = menu.findItem(choice);
            order.addItem(item, inventory);
        }
        order.showOrder();
    }

    void run() {
        setupMenu();
        setupInventory();
        int option;
        while (true) {
            cout << "\n----- Restaurant Management -----\n";
            cout << "1. View Menu\n2. Place Order\n3. View Inventory\n4. Exit\nChoose an option: ";
            cin >> option;
            cin.ignore();
            switch (option) {
                case 1:
                    menu.displayMenu();
                    break;
                case 2:
                    takeOrder();
                    break;
                case 3:
                    inventory.showStock();
                    break;
                case 4:
                    cout << "Exiting...\n";
                    return;
                default:
                    cout << "Invalid option. Try again.\n";
            }
        }
    }
};

int main() {
    RestaurantManagementSystem rms;
    rms.run();
    return 0;
}
