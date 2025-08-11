#include <bits/stdc++.h>
using namespace std;

static long double round2(long double x) {
    return floor(x * 100.0L + 0.5L) / 100.0L;
}

using UserId = int;
using GroupId = int;

struct User {
    UserId id;
    string name;
    string email;
    User() = default;
    User(UserId id_, string name_, string email_ = "") : id(id_), name(move(name_)), email(move(email_)) {}
};

struct Group {
    GroupId id;
    string name;
    unordered_set<UserId> members;
    Group() = default;
    Group(GroupId id_, string name_) : id(id_), name(move(name_)) {}
};

struct Expense {
    UserId payer;
    long double amount;
    string description;
    vector<UserId> participants; // list of participants (including payer if payer part of split)
    vector<long double> owedAmounts; // owedAmounts[i] corresponds to participants[i]
    time_t timestamp;
    Expense(UserId p, long double a, string desc, vector<UserId> parts, vector<long double> owed)
        : payer(p), amount(a), description(move(desc)), participants(move(parts)), owedAmounts(move(owed)) {
        timestamp = time(nullptr);
    }
};

/*** Split abstraction and concrete splits ***/
struct Split {
    virtual ~Split() = default;
    // returns owed amounts aligned with participants vector
    virtual vector<long double> computeAmounts(long double total, const vector<UserId>& participants, UserId payer) const = 0;
};

struct EqualSplit : Split {
    vector<long double> computeAmounts(long double total, const vector<UserId>& participants, UserId) const override {
        int n = participants.size();
        vector<long double> res(n, 0.0L);
        if (n == 0) return res;
        long double equal = floor((total / n) * 100.0L + 0.5L) / 100.0L; // round to cents
        for (int i = 0; i < n; ++i) res[i] = equal;
        // fix remainder cents due to rounding
        long double sum = equal * n;
        long double diff = round2(total - sum);
        int idx = 0;
        while (fabsl(diff) >= 0.005L) {
            res[idx++] += 0.01L;
            diff = round2(diff - 0.01L);
            if (idx >= n) idx = 0;
        }
        return res;
    }
};

struct PercentSplit : Split {
    vector<long double> percents; // size must match participants
    explicit PercentSplit(vector<long double> p) : percents(move(p)) {}
    vector<long double> computeAmounts(long double total, const vector<UserId>& participants, UserId) const override {
        int n = participants.size();
        vector<long double> res(n, 0.0L);
        long double sumPct = 0.0L;
        for (auto v : percents) sumPct += v;
        if (n != (int)percents.size()) throw runtime_error("PercentSplit: participants size mismatch");
        if (fabsl(sumPct - 100.0L) > 1e-6L) throw runtime_error("PercentSplit: percents must sum to 100");
        // compute and round; fix remainder like equal split
        long double sum = 0.0L;
        for (int i = 0; i < n; ++i) {
            res[i] = round2(total * (percents[i] / 100.0L));
            sum += res[i];
        }
        long double diff = round2(total - sum);
        int idx = 0;
        while (fabsl(diff) >= 0.005L) {
            res[idx++] += 0.01L;
            diff = round2(diff - 0.01L);
            if (idx >= n) idx = 0;
        }
        return res;
    }
};

struct ExactSplit : Split {
    vector<long double> amounts; // exact amounts per participant
    explicit ExactSplit(vector<long double> a) : amounts(move(a)) {}
    vector<long double> computeAmounts(long double total, const vector<UserId>& participants, UserId) const override {
        if ((int)amounts.size() != (int)participants.size()) throw runtime_error("ExactSplit: size mismatch");
        long double sum = 0.0L;
        for (auto v : amounts) sum += v;
        if (fabsl(round2(sum) - round2(total)) > 1e-6L) throw runtime_error("ExactSplit: amounts must sum to total");
        vector<long double> res = amounts;
        // Round each
        for (auto &r : res) r = round2(r);
        return res;
    }
};

/*** Core service ***/
class SplitwiseService {
private:
    unordered_map<UserId, User> users;
    unordered_map<GroupId, Group> groups;
    vector<Expense> expenses;
    // balances[a][b] = amount a owes b
    unordered_map<UserId, unordered_map<UserId, long double>> balances;
    UserId nextUserId = 1;
    GroupId nextGroupId = 1;

    // Add debt: debtor owes creditor "amt". Normalize against reverse if present.
    void addDebt(UserId debtor, UserId creditor, long double amt) {
        amt = round2(amt);
        if (amt <= 0.0L) return;
        // If creditor already owes debtor, cancel
        long double &rev = balances[creditor][debtor];
        if (rev > 0.0L) {
            if (rev >= amt) {
                rev = round2(rev - amt);
                if (rev < 0.005L) balances[creditor].erase(debtor);
                return;
            } else {
                // rev < amt
                amt = round2(amt - rev);
                balances[creditor].erase(debtor);
            }
        }
        balances[debtor][creditor] = round2(balances[debtor][creditor] + amt);
    }

public:
    UserId createUser(const string &name, const string &email = "") {
        UserId id = nextUserId++;
        users.emplace(id, User(id, name, email));
        return id;
    }

    GroupId createGroup(const string &name, const vector<UserId>& memberIds) {
        GroupId gid = nextGroupId++;
        groups.emplace(gid, Group(gid, name));
        for (auto uid : memberIds) groups[gid].members.insert(uid);
        return gid;
    }

    void addUserToGroup(GroupId gid, UserId uid) {
        auto it = groups.find(gid);
        if (it == groups.end()) throw runtime_error("Group not found");
        it->second.members.insert(uid);
    }

    // Add expense given a Split object (unique_ptr)
    void addExpense(UserId payer, long double amount, const string &description, const vector<UserId>& participants, const Split &splitStrategy) {
        if (!users.count(payer)) throw runtime_error("Payer not found");
        if (participants.empty()) throw runtime_error("Participants empty");
        for (auto u : participants) if (!users.count(u)) throw runtime_error("Unknown participant");

        vector<long double> owed = splitStrategy.computeAmounts(amount, participants, payer);
        if ((int)owed.size() != (int)participants.size()) throw runtime_error("Split result mismatch");

        // Save expense
        expenses.emplace_back(payer, amount, description, participants, owed);

        // Update balances: for each participant p != payer, p owes payer owed_i
        for (size_t i = 0; i < participants.size(); ++i) {
            UserId part = participants[i];
            long double owedAmt = round2(owed[i]);
            // If participant is payer, they effectively contributed their share; no debt
            if (part == payer) continue;
            if (owedAmt <= 0.0L) continue;
            addDebt(part, payer, owedAmt);
        }
    }

    // Settle: debtor pays creditor amount
    void settle(UserId debtor, UserId creditor, long double amount) {
        amount = round2(amount);
        if (amount <= 0.0L) return;
        addDebt(creditor, debtor, amount); // creditor will owe debtor negative; but addDebt handles normalization
        // To interpret settle: debtor pays creditor -> debtor's debt to creditor reduces.
        // We can implement as reducing balances[debtor][creditor] by amount:
        auto it = balances.find(debtor);
        if (it != balances.end()) {
            long double &d = it->second[creditor];
            if (d > 0.0L) {
                if (d > amount + 1e-9L) {
                    d = round2(d - amount);
                    amount = 0.0L;
                } else {
                    amount = round2(amount - d);
                    it->second.erase(creditor);
                }
            }
        }
        // if any leftover amount (meaning debtor paid more than they owed), we should create reverse credit (creditor owes debtor)
        if (amount > 0.0L) {
            addDebt(creditor, debtor, amount);
        }
    }

    // View balances for a user
    void showBalancesFor(UserId uid) const {
        if (!users.count(uid)) { cout << "User not found\n"; return;}
        bool any = false;
        // Debts where uid owes others
        auto it = balances.find(uid);
        if (it != balances.end()) {
            for (auto &kv : it->second) {
                if (kv.second > 0.0L) {
                    any = true;
                    cout << users.at(uid).name << " owes " << users.at(kv.first).name << " : " << fixed << setprecision(2) << (double)kv.second << "\n";
                }
            }
        }
        // Debts where others owe uid
        for (auto &outer : balances) {
            UserId other = outer.first;
            if (other == uid) continue;
            auto it2 = outer.second.find(uid);
            if (it2 != outer.second.end() && it2->second > 0.0L) {
                any = true;
                cout << users.at(other).name << " owes " << users.at(uid).name << " : " << fixed << setprecision(2) << (double)it2->second << "\n";
            }
        }
        if (!any) cout << "No balances for " << users.at(uid).name << "\n";
    }

    // Show global non-zero balances
    void showAllBalances() const {
        bool any = false;
        for (auto &outer : balances) {
            UserId a = outer.first;
            for (auto &inner : outer.second) {
                UserId b = inner.first;
                long double amt = inner.second;
                if (amt > 0.0L) {
                    cout << users.at(a).name << " owes " << users.at(b).name << " : " << fixed << setprecision(2) << (double)amt << "\n";
                    any = true;
                }
            }
        }
        if (!any) cout << "No balances\n";
    }

    // Get specific net balance between two users: positive => u1 owes u2 by amount
    long double getNetBalance(UserId u1, UserId u2) const {
        long double a = 0.0L, b = 0.0L;
        auto it = balances.find(u1);
        if (it != balances.end()) {
            auto it2 = it->second.find(u2);
            if (it2 != it->second.end()) a = it2->second;
        }
        it = balances.find(u2);
        if (it != balances.end()) {
            auto it2 = it->second.find(u1);
            if (it2 != it->second.end()) b = it2->second;
        }
        long double net = round2(a - b);
        return net;
    }

    // Optional: show group expenses for a group
    void showGroupExpenses(GroupId gid) const {
        auto git = groups.find(gid);
        if (git == groups.end()) { cout << "Group not found\n"; return; }
        cout << "Expenses for group '" << git->second.name << "':\n";
        for (const auto &e : expenses) {
            // consider expense if all participants subset of group (simple filter)
            bool anyInGroup = false;
            for (auto p : e.participants) if (git->second.members.count(p)) { anyInGroup = true; break; }
            if (!anyInGroup) continue;
            cout << "- [" << e.description << "] payer: " << users.at(e.payer).name << ", total: " << fixed << setprecision(2) << (double)e.amount << "\n";
        }
    }

    // For demo/testing: list users
    void listUsers() const {
        for (auto &kv : users) {
            cout << kv.first << ": " << kv.second.name << " (" << kv.second.email << ")\n";
        }
    }
};

///// Demo usage in main
int main() {
    ios::sync_with_stdio(false);
    cin.tie(nullptr);

    SplitwiseService svc;

    // create users
    UserId alice = svc.createUser("Alice", "alice@example.com");
    UserId bob   = svc.createUser("Bob",   "bob@example.com");
    UserId carol = svc.createUser("Carol", "carol@example.com");
    UserId dave  = svc.createUser("Dave",  "dave@example.com");

    // create a group
    GroupId g1 = svc.createGroup("Trip", {alice, bob, carol, dave});

    cout << "Users:\n"; svc.listUsers(); cout << "\n";

    // 1) Equal split: Alice pays 120 split equally among 4
    {
        vector<UserId> parts = {alice, bob, carol, dave};
        EqualSplit eq;
        svc.addExpense(alice, 120.00L, "Dinner", parts, eq);
    }

    cout << "After DinnerExpense (equal split of 120 by Alice):\n";
    svc.showAllBalances();
    cout << "\n";

    // 2) Percent split: Bob pays 200, percents: Bob 50%, Carol 30%, Dave 20% (Bob also participant)
    {
        vector<UserId> parts = {bob, carol, dave};
        PercentSplit ps({50.0L, 30.0L, 20.0L});
        svc.addExpense(bob, 200.00L, "Hotel", parts, ps);
    }

    cout << "After HotelExpense (percent split by Bob):\n";
    svc.showAllBalances();
    cout << "\n";

    // 3) Exact split: Carol pays 90, participants: Alice owes 30, Bob owes 30, Carol pays 30 (exact)
    {
        vector<UserId> parts = {alice, bob, carol};
        ExactSplit ex({30.0L, 30.0L, 30.0L});
        svc.addExpense(carol, 90.0L, "Taxi", parts, ex);
    }

    cout << "After TaxiExpense (exact split by Carol):\n";
    svc.showAllBalances();
    cout << "\n";

    // Check single user's balances
    cout << "Balances for Bob:\n";
    svc.showBalancesFor(bob);
    cout << "\n";

    // Settle: Bob pays Alice 30
    cout << "Bob settles 30 to Alice\n";
    svc.settle(bob, alice, 30.0L);
    svc.showAllBalances();
    cout << "\n";

    // Net between two users
    cout << "Net Bob -> Alice : " << fixed << setprecision(2) << (double)svc.getNetBalance(bob, alice) << "\n";

    return 0;
}
