import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

enum Priority {
    LOW,
    MEDIUM,
    HIGH
}

enum Status {
    PENDING,
    IN_PROGRESS,
    COMPLETED
}

class User {
    String userId;
    String name;
    
    User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }
}

class Task {
    long id;
    String title;
    String description;
    LocalDateTime dueDate;
    Priority priority;
    Status status;
    User assignedTo;
    
    Task(long id,String title,String description, LocalDateTime dueDate, Priority priority) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = Status.PENDING;
    }
    
    @Override
    public String toString() {
        return "[" + id + "] " + title + " (" + status + ", " + priority +
                ", assignedTo=" + (assignedTo == null ? "none" : assignedTo.name) + ")";
    }
}

class TaskRepository {
    private final Map<Long,Task> store = new HashMap<>();
    
    void save(Task task) {
        store.put(task.id, task);
    }
    
    Optional<Task> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }
    
    void delete(long id) {
        store.remove(id);
    }
    
    Collection<Task> findAll() {
        return store.values();
    }
}

class TaskService {
    private final TaskRepository repo;
    private long idCounter = 1;

    TaskService(TaskRepository repo) {
        this.repo = repo;
    }

    Task createTask(String title, String desc, LocalDateTime due, Priority priority) {
        Task t = new Task(idCounter++, title, desc, due, priority);
        repo.save(t);
        return t;
    }

    boolean updateTask(long id, String title, String desc, Priority priority, LocalDateTime dueDate) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;

        Task t = opt.get();
        if (title != null) t.title = title;
        if (desc != null) t.description = desc;
        if (priority != null) t.priority = priority;
        if (dueDate != null) t.dueDate = dueDate;
        return true;
    }

    boolean deleteTask(long id) {
        if (repo.findById(id).isEmpty()) return false;
        repo.delete(id);
        return true;
    }

    boolean assignTask(long taskId, User user) {
        Optional<Task> opt = repo.findById(taskId);
        if (opt.isEmpty()) return false;

        Task t = opt.get();
        t.assignedTo = user;
        return true;
    }

    boolean markCompleted(long id) {
        Optional<Task> opt = repo.findById(id);
        if (opt.isEmpty()) return false;

        opt.get().status = Status.COMPLETED;
        return true;
    }

    List<Task> search(String keyword, Priority priority, Status status, User assigned) {
        return repo.findAll().stream()
                .filter(t -> keyword == null || t.title.toLowerCase().contains(keyword.toLowerCase()))
                .filter(t -> priority == null || t.priority == priority)
                .filter(t -> status == null || t.status == status)
                .filter(t -> assigned == null || (t.assignedTo != null && t.assignedTo.userId.equals(assigned.userId)))
                .collect(Collectors.toList());
    }
}

public class TaskManagementDemo {
    public static void main(String[] args) {
        TaskRepository repo = new TaskRepository();
        TaskService service = new TaskService(repo);

        User alice = new User("u1", "Alice");
        User bob   = new User("u2", "Bob");

        Task t1 = service.createTask("Build API", "Implement endpoints", LocalDateTime.now().plusDays(3), Priority.HIGH);
        Task t2 = service.createTask("Fix Bugs", "Resolve UI bugs", LocalDateTime.now().plusDays(1), Priority.MEDIUM);

        service.assignTask(t1.id, alice);
        service.updateTask(t2.id, "Fix UI Bugs", null, Priority.HIGH, null);
        service.markCompleted(t2.id);

        System.out.println("\nAll Tasks:");
        repo.findAll().forEach(System.out::println);

        System.out.println("\nSearch for HIGH priority:");
        service.search(null, Priority.HIGH, null, null).forEach(System.out::println);
    }
}