// Card → represents the ATM card (cardNumber, PIN, linked account).

// Account → represents a user’s bank account (account number, balance).

// BankSystem → backend system that validates accounts, processes deposits, withdrawals, balance inquiry.

// ATM → main facade class where users interact, manages authentication, transactions, and talks to BankSystem.

// CashDispenser → dispenses money physically.

// ATMInterface → simple simulation of user interaction

import java.util.*;

// --- Account ---
class Account {
    private String accountNumber;
    private double balance;
    
    public Account(String accountNumber, double balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public synchronized double getBalance() {  
        return balance;
    }
    
    public synchronized void deposit(double amount) {
        balance += amount;
    }
    
    public synchronized boolean withdraw(double amount) {
        if (amount <= balance) {
            balance -= amount;
            return true;
        }
        return false;
    }
}

// --- Card ---
class Card {
    private String cardNumber;
    private String pin;
    private Account linkedAccount;
    
    public Card(String cardNumber, String pin, Account account) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.linkedAccount = account;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public String getPin() {
        return pin;
    }
    
    public Account getLinkedAccount() {
        return linkedAccount;
    }
}

// --- BankSystem (Backend) ---
class BankSystem {
    private Map<String, Card> cards = new HashMap<>();

    public void addCard(Card card) {
        cards.put(card.getCardNumber(), card);
    }

    public boolean validateCard(String cardNumber, String pin) {
        Card card = cards.get(cardNumber);
        return card != null && card.getPin().equals(pin);
    }

    public Account getAccount(String cardNumber) {
        return cards.get(cardNumber).getLinkedAccount();
    }

    public Card getCard(String cardNumber) {   
        return cards.get(cardNumber);
    }
}

// --- CashDispenser ---
class CashDispenser {
    public void dispenseCash(double amount) {
        System.out.println("Dispensing cash: $" + amount);
    }
}

// --- ATM ---
class ATM {
    private BankSystem bankSystem;
    private CashDispenser cashDispenser;
    private Card currentCard;
    
    public ATM(BankSystem bankSystem) {
        this.bankSystem = bankSystem;
        this.cashDispenser = new CashDispenser();
    }

    public boolean authenticate(String cardNumber, String pin) {
        boolean isValid = bankSystem.validateCard(cardNumber, pin);
        if (isValid) {
            currentCard = bankSystem.getCard(cardNumber); 
        }
        return isValid;
    }

    public void checkBalance() {
        if (currentCard != null) {
            System.out.println("Balance: $" + currentCard.getLinkedAccount().getBalance());  
        }
    }
    
    public void deposit(double amount) {
        if (currentCard != null) {
            currentCard.getLinkedAccount().deposit(amount);
            System.out.println("Deposited: $" + amount);
        }
    }

    public void withdraw(double amount) {
        if (currentCard != null) {
            Account acc = currentCard.getLinkedAccount();
            if (acc.withdraw(amount)) {
                cashDispenser.dispenseCash(amount);
            } else {
                System.out.println("Insufficient funds!");
            }
        }
    }

    public void ejectCard() {
        currentCard = null;
        System.out.println("Card ejected.");
    }
}

// --- ATMInterface (User Simulation) ---
public class ATMSimulation {
    public static void main(String[] args) {
        // Setup backend
        BankSystem bankSystem = new BankSystem();
        Account acc1 = new Account("ACC123", 1000.0);
        Card card1 = new Card("CARD123", "1234", acc1);
        bankSystem.addCard(card1);

        // ATM Machine
        ATM atm = new ATM(bankSystem);

        Scanner scanner = new Scanner(System.in);
        System.out.print("Insert card number: ");
        String cardNum = scanner.nextLine();
        System.out.print("Enter PIN: ");
        String pin = scanner.nextLine();

        if (atm.authenticate(cardNum, pin)) {
            boolean exit = false;
            while (!exit) {
                System.out.println("\n1. Balance Inquiry\n2. Deposit\n3. Withdraw\n4. Exit");
                int choice = scanner.nextInt();

                switch (choice) {
                    case 1:
                        atm.checkBalance();
                        break;
                    case 2:
                        System.out.print("Enter amount to deposit: ");
                        double dep = scanner.nextDouble();
                        atm.deposit(dep);
                        break;
                    case 3:
                        System.out.print("Enter amount to withdraw: ");
                        double wd = scanner.nextDouble();
                        atm.withdraw(wd);
                        break;
                    case 4:
                        atm.ejectCard();
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option.");
                }
            }
        } else {
            System.out.println("Authentication failed!");
        }

        scanner.close();  
    }
}
