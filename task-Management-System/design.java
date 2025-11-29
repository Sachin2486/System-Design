/// ---- Start ----////
// Requirement : 
// Design a Task Management System that allows users to create, assign, update, and track tasks. 
// The system should support multiple users, task priorities, due dates, and status tracking. 
// Users should be able to categorize tasks, add comments, and receive notifications for task updates.
/// ----- END -----////


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}

enum Priority {
    LOW,
    MEDIUM,
    HIGH
}

class User {
    private final String name;
    private final String email;
    
    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public String getEmail() {
        return email;
    }
}

class Comment {
    private final User user;
    private final String text;
    private final Date createdAt;
    
    public Comment(User user, String text) {
        this.user = user;
        this.text = text;
        this.createdAt = new Date();
    }
    
    public String toString() {
        return user.getName() + " (" + createdAt + "): " + text;
    }
}

class Task {
    private final int id;
    private String title;
    private String description;
    private User createdBy;
    private User assignedTo;
    private TaskStatus status;
    private Priority priority;
    private Date dueDate;
    private String category;

    private final List<Comment> comments = new ArrayList<>();

    public Task(int id, String title, String description, User createdBy,
                Priority priority, Date dueDate, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdBy = createdBy;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = TaskStatus.TODO;
        this.category = category;
    }

    public int getId() { return id; }
    public TaskStatus getStatus() { return status; }
    public User getAssignedTo() { return assignedTo; }

    public void assignTo(User user) {
        this.assignedTo = user;
    }

    public void updateStatus(TaskStatus status) {
        this.status = status;
    }

    public void addComment(Comment c) {
        comments.add(c);
    }

    public void printTask() {
        System.out.println("\nTask ID: " + id +
                "\nTitle: " + title +
                "\nAssigned To: " + (assignedTo == null ? "None" : assignedTo.getName()) +
                "\nStatus: " + status +
                "\nPriority: " + priority +
                "\nCategory: " + category +
                "\nDue: " + dueDate +
                "\nComments:");
        comments.forEach(c -> System.out.println(" - " + c));
    }
}

class NotificationService {

    public void notify(User user, String msg) {
        System.out.println("[NOTIFY] -> " + user.getName() + ": " + msg);
    }
}

class TaskManager {

    private final Map<Integer, Task> tasks = new ConcurrentHashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final NotificationService notifier = new NotificationService();

    private int taskIdCounter = 1;

    public Task createTask(String title, String desc, User createdBy,
                           Priority priority, Date due, String category) {

        lock.lock();
        try {
            int id = taskIdCounter++;
            Task t = new Task(id, title, desc, createdBy, priority, due, category);
            tasks.put(id, t);

            notifier.notify(createdBy, "Task '" + title + "' created with ID: " + id);
            return t;
        } finally {
            lock.unlock();
        }
    }

    public void assignTask(int taskId, User user) {
        Task task = tasks.get(taskId);
        if (task == null) return;

        task.assignTo(user);
        notifier.notify(user, "You have been assigned Task ID: " + taskId);
    }

    public void updateStatus(int taskId, TaskStatus status, User user) {
        Task task = tasks.get(taskId);
        if (task == null) return;

        task.updateStatus(status);
        notifier.notify(user, "Status updated for Task ID: " + taskId + " → " + status);
    }

    public void addComment(int taskId, User user, String text) {
        Task task = tasks.get(taskId);
        if (task == null) return;

        task.addComment(new Comment(user, text));
        notifier.notify(user, "Comment added to Task ID: " + taskId);
    }

    public void printTask(int taskId) {
        Task t = tasks.get(taskId);
        if (t != null) t.printTask();
    }
}

public class TaskManagementDemo {

    public static void main(String[] args) {

        TaskManager manager = new TaskManager();

        // Users
        User sachin = new User("Sachin", "sachin@mail.com");
        User rohit = new User("Rohit", "rohit@mail.com");
        User kiran = new User("Kiran", "kiran@mail.com");

        // Create Tasks
        Task t1 = manager.createTask(
                "Fix Login Bug",
                "Users unable to login with OTP.",
                sachin,
                Priority.HIGH,
                new Date(),
                "Engineering"
        );

        Task t2 = manager.createTask(
                "Prepare Presentation",
                "Make slides for Sprint Demo.",
                rohit,
                Priority.MEDIUM,
                new Date(),
                "Product"
        );

        // Assign Tasks
        manager.assignTask(t1.getId(), kiran);
        manager.assignTask(t2.getId(), sachin);

        // Update Task Status
        manager.updateStatus(t1.getId(), TaskStatus.IN_PROGRESS, kiran);

        // Add Comments
        manager.addComment(t1.getId(), kiran, "Started debugging. Logs collected.");
        manager.addComment(t1.getId(), sachin, "Share logs with me.");

        // Display Task
        manager.printTask(t1.getId());
        manager.printTask(t2.getId());
    }
}