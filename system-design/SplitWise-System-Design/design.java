import java.util.*;

// --- User ---
class User {
	private String id;
	private String name;
	private String email;
	private Map<String, Double> balances; // otherUserId -> amount

	public User(String id, String name,String email) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.balances = new HashMap<>();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Map<String, Double> getBalances() {
		return balances;
	}

	public void updateBalance(String userId, double amount) {
		balances.put(userId, balances.getOrDefault(userId, 0.0) + amount);
	}
}

// --- Split Strategy ---
interface SplitStrategy {
	Map<User, Double> split(double amount, List<User> participants, List<Double> values);
}

class EqualSplit implements SplitStrategy {
	public Map<User, Double> split(double amount, List<User> participants, List<Double> values) {
		Map<User, Double> map = new HashMap<>();
		double share = amount / participants.size();
		for (User u : participants) {
			map.put(u, share);
		}
		return map;
	}
}

class PercentageSplit implements SplitStrategy {
	public Map<User, Double> split(double amount, List<User> participants, List<Double> values) {
		Map<User, Double> map = new HashMap<>();
		for (int i = 0; i < participants.size(); i++) {
			double share = amount * (values.get(i) / 100.0);
			map.put(participants.get(i), share);
		}
		return map;
	}
}

// Exact Amount Split
class ExactSplit implements SplitStrategy {
	public Map<User, Double> split(double amount, List<User> participants, List<Double> values) {
		Map<User, Double> map = new HashMap<>();
		for (int i = 0; i < participants.size(); i++) {
			map.put(participants.get(i), values.get(i));
		}
		return map;
	}
}

// Expense

class Expense {
	private String description;
	private double amount;
	private User paidBy;
	private List<User> participants;
	private SplitStrategy splitStrategy;
	private List<Double> values;

	public Expense(String description, double amount, User paidBy, List<User> participants,
	               SplitStrategy strategy, List<Double> values)
	{
		this.description = description;
		this.amount = amount;
		this.paidBy = paidBy;
		this.participants = participants;
		this.splitStrategy = strategy;
		this.values = values;
	}
	
	 public void applyExpense() {
        Map<User, Double> shares = splitStrategy.split(amount, participants, values);

        for (User participant : participants) {
            double share = shares.get(participant);
            if (!participant.getId().equals(paidBy.getId())) {
                participant.updateBalance(paidBy.getId(), share);   // owes to payer
                paidBy.updateBalance(participant.getId(), -share); // payer is owed
            }
        }
    }

    @Override
    public String toString() {
        return description + " of $" + amount + " paid by " + paidBy.getName();
    }
}

// --- Group ---
class Group {
    private String id;
    private String name;
    private List<User> users;
    private List<Expense> expenses;

    public Group(String id, String name) {
        this.id = id;
        this.name = name;
        this.users = new ArrayList<>();
        this.expenses = new ArrayList<>();
    }
    
     public void addUser(User user) { users.add(user); }
    public List<User> getUsers() { return users; }

    public void addExpense(Expense expense) {
        expenses.add(expense);
        expense.applyExpense();
    }

    public void showExpenses() {
        for (Expense e : expenses) {
            System.out.println(e);
        }
    }
}

// --- ExpenseManager (Facade) ---
class ExpenseManager {
    private Map<String, User> users = new HashMap<>();
    private Map<String, Group> groups = new HashMap<>();

    public User createUser(String id, String name, String email) {
        User user = new User(id, name, email);
        users.put(id, user);
        return user;
    }

    public Group createGroup(String id, String name) {
        Group group = new Group(id, name);
        groups.put(id, group);
        return group;
    }

    public void showBalances(User user) {
        System.out.println("Balances for " + user.getName() + ":");
        for (Map.Entry<String, Double> entry : user.getBalances().entrySet()) {
            double amount = entry.getValue();
            if (amount > 0) {
                System.out.println("  Owes " + users.get(entry.getKey()).getName() + ": $" + amount);
            } else if (amount < 0) {
                System.out.println("  Is owed by " + users.get(entry.getKey()).getName() + ": $" + (-amount));
            }
        }
    }
}

// --- Demo ---
public class SplitwiseDemo {
    public static void main(String[] args) {
        ExpenseManager manager = new ExpenseManager();

        // Create users
        User u1 = manager.createUser("U1", "Alice", "alice@mail.com");
        User u2 = manager.createUser("U2", "Bob", "bob@mail.com");
        User u3 = manager.createUser("U3", "Charlie", "charlie@mail.com");

        // Create group
        Group g1 = manager.createGroup("G1", "Trip");
        g1.addUser(u1);
        g1.addUser(u2);
        g1.addUser(u3);

        // Expense 1: Equal Split
        Expense e1 = new Expense("Lunch", 90, u1, Arrays.asList(u1, u2, u3), new EqualSplit(), null);
        g1.addExpense(e1);

        // Expense 2: Percentage Split
        Expense e2 = new Expense("Hotel", 300, u2, Arrays.asList(u1, u2, u3), new PercentageSplit(), Arrays.asList(40.0, 20.0, 40.0));
        g1.addExpense(e2);

        // Expense 3: Exact Split
        Expense e3 = new Expense("Cab", 150, u3, Arrays.asList(u1, u2, u3), new ExactSplit(), Arrays.asList(50.0, 50.0, 50.0));
        g1.addExpense(e3);

        // Show group expenses
        System.out.println("\nGroup Expenses:");
        g1.showExpenses();

        // Show balances
        System.out.println("\nBalances:");
        manager.showBalances(u1);
        manager.showBalances(u2);
        manager.showBalances(u3);
    }
}


