#include <bits/stdc++.h>
#include <iostream>

using namespace std;

struct Customer {
    string id;
    Customer (string id) : id(id) {}
};

struct Merchant {
    string id;
    Merchant (string id) : id(id) {}
};

struct Cart {
    double total;
    Customer customer;
    Merchant merchant;
    
    Cart(double total, Customer customer, Merchant merchant) :
    total(total), customer(customer), merchant(merchant) {}
};

// ------------- DISCOUNT STRATEGY ----------------
class DiscountStrategy {
    public:
    virtual double getDiscount(double cartValue) const = 0;
    virtual ~DiscountStrategy() = default;
};

class FlatDiscount : public DiscountStrategy {
    double amount;
    
    public:
    FlatDiscount(double amt) : amount(amt) {}
    double getDiscount(double cartValue) const override {
        return min(cartValue, amount);
    }
};

class PercentageDiscount : public DiscountStrategy {
    double percent;
    double maxAmount;
public:
    PercentageDiscount(double p, double maxAmt) : percent(p), maxAmount(maxAmt) {}
    double getDiscount(double cartValue) const override {
        double discount = cartValue * (percent / 100.0);
        return min(discount, maxAmount);
    }
};

// ------------- APPLICABILITY RULES ----------------
class ApplicabilityRule {
public:
    virtual bool isApplicable(const Cart& cart, int usageCount) const = 0;
    virtual ~ApplicabilityRule() = default;
};

class MinCartValueRule : public ApplicabilityRule {
    double minValue;
public:
    MinCartValueRule(double minValue) : minValue(minValue) {}
    bool isApplicable(const Cart& cart, int) const override {
        return cart.total >= minValue;
    }
};

class CustomerRule : public ApplicabilityRule {
    unordered_set<string> allowedCustomers;
public:
    CustomerRule(const vector<string>& ids) {
        for (const auto& id : ids) allowedCustomers.insert(id);
    }
    bool isApplicable(const Cart& cart, int) const override {
        return allowedCustomers.empty() || allowedCustomers.count(cart.customer.id);
    }
};

class MerchantRule : public ApplicabilityRule {
    unordered_set<string> allowedMerchants;
public:
    MerchantRule(const vector<string>& ids) {
        for (const auto& id : ids) allowedMerchants.insert(id);
    }
    bool isApplicable(const Cart& cart, int) const override {
        return allowedMerchants.empty() || allowedMerchants.count(cart.merchant.id);
    }
};

class UsageRule : public ApplicabilityRule {
    int maxUsage; // -1 for unlimited
public:
    UsageRule(int max) : maxUsage(max) {}
    bool isApplicable(const Cart&, int usageCount) const override {
        return maxUsage == -1 || usageCount < maxUsage;
    }
};

// ------------- COUPON CLASS ----------------

class Coupon {
    string code;
    shared_ptr<DiscountStrategy> discount;
    vector<shared_ptr<ApplicabilityRule>> rules;
    
    mutable unordered_map<string,int> usageMap; // customer.id -> count
    
    public:
    
    Coupon(string code, shared_ptr<DiscountStrategy> discount) :
     code(code), discount(discount) {}
    
    void addRule(shared_ptr<ApplicabilityRule> rule) {
        rules.push_back(rule);
    }

    bool canApply(const Cart& cart) const {
        int usage = usageMap[cart.customer.id];
        for (auto& rule : rules) {
            if (!rule->isApplicable(cart, usage)) return false;
        }
        return true;
    }
    
    double applyCoupon(const Cart& cart) {
        if (!canApply(cart)) {
            cout << "Coupon not applicable for customer " << cart.customer.id << "\n";
            return 0.0;
        }
        double discountValue = discount->getDiscount(cart.total);
        usageMap[cart.customer.id]++;
        return discountValue;
    }

    string getCode() const { return code; }
};

int main() {
    Customer user1("cust1"), user2("cust2");
    Merchant m1("amazon"), m2("flipkart");

    Cart cart1(1000, user1, m1);
    Cart cart2(500, user2, m2);

    // % coupon - 10% upto ₹200
    auto percentDiscount = make_shared<PercentageDiscount>(10, 200);
    Coupon percentCoupon("SAVE10", percentDiscount);
    percentCoupon.addRule(make_shared<MinCartValueRule>(300));
    percentCoupon.addRule(make_shared<CustomerRule>(vector<string>{ "cust1", "cust2" }));
    percentCoupon.addRule(make_shared<MerchantRule>(vector<string>{ "amazon", "flipkart" }));
    percentCoupon.addRule(make_shared<UsageRule>(1));  // one time use

    // flat coupon - ₹150
    auto flatDiscount = make_shared<FlatDiscount>(150);
    Coupon flatCoupon("FLAT150", flatDiscount);
    flatCoupon.addRule(make_shared<UsageRule>(-1)); // unlimited usage
    flatCoupon.addRule(make_shared<MinCartValueRule>(400));
    
    cout << "Applying SAVE10 to cart1: ₹" << percentCoupon.applyCoupon(cart1) << "\n";
    cout << "Reapplying SAVE10 to cart1: ₹" << percentCoupon.applyCoupon(cart1) << "\n";
    cout << "Applying FLAT150 to cart2: ₹" << flatCoupon.applyCoupon(cart2) << "\n";

    return 0;
}
