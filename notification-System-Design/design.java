import java.util.*;

interface Notification {
    void send(String message);
    String getMessage();
}

// Basic Notification Implementation
class NotificationImpl implements Notification {
    private String message;

    public NotificationImpl() {}

    @Override
    public void send(String message) {
        this.message = message;
        System.out.println("Base Notification: " + message);
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}

// Base Decorator
abstract class BaseDecorator implements Notification {
    protected Notification wrapped;
    private String message;

    public BaseDecorator(Notification wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void send(String message) {
        wrapped.send(message);
        this.message = wrapped.getMessage();
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}

// SMS Decorator
class SMSDecorator extends BaseDecorator {
    private final List<Long> numbers;

    public SMSDecorator(Notification wrappee, List<Long> numbers) {
        super(wrappee);
        this.numbers = numbers;
    }

    @Override
    public void send(String message) {
        super.send(message);
        String formattedMessage = format();
        for (Long number : numbers) {
            // In real-world: send SMS logic here
            System.out.println("SMS sent to " + number + ": " + formattedMessage);
        }
    }

    private String format() {
        return super.getMessage() + " [via SMS]";
    }
}

// Email Decorator
class EmailDecorator extends BaseDecorator {
    private final List<String> emails;

    public EmailDecorator(Notification wrappee, List<String> emails) {
        super(wrappee);
        this.emails = emails;
    }

    @Override
    public void send(String message) {
        super.send(message);
        String formattedMessage = format();
        for (String email : emails) {
            // In real-world: send Email logic here
            System.out.println("Email sent to " + email + ": " + formattedMessage);
        }
    }

    private String format() {
        return super.getMessage() + " [via Email]";
    }
}

// Test the Notification System
public class NotificationSystemDemo {
    public static void main(String[] args) {
        Notification baseNotification = new NotificationImpl();

        List<Long> numbers = Arrays.asList(9876543210L, 9123456789L);
        List<String> emails = Arrays.asList("alice@example.com", "bob@example.com");

        // Wrap the base notification with both Email and SMS decorators
        Notification smsDecorator = new SMSDecorator(baseNotification, numbers);
        Notification emailDecorator = new EmailDecorator(smsDecorator, emails);

        // Send notification (goes through SMS + Email layers)
        emailDecorator.send("System Alert: Server down!");
    }
}
