
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;


enum Priority { LOW, MEDIUM, HIGH }
enum TaskStatus { PENDING, IN_PROGRESS, COMPLETED }

class User {
	private final String userId;
	private final String name;

	public User(String userId, String name) {
		this.userId = userId;
		this.name = name;
	}
	public String getUserId() {
		return userId;
	}
	public String getName() {
		return name;
	}
}

class TaskHistory {
	private final String taskId;
	private final String updateLog;
	private final LocalDateTime timestamp = LocalDateTime.now();

	public TaskHistory(String taskId, String updateLog) {
		this.taskId = taskId;
		this.updateLog = updateLog;
	}
	@Override
	public String toString() {
		return "[" + timestamp + "] Task ID " + taskId + " -> " + updateLog;
	}
}

class Reminder {
	private final LocalDateTime alertTime;

	public Reminder(LocalDateTime alertTime) {
		this.alertTime = alertTime;
	}
	@Override
	public String toString() {
		return "Reminder set for: " + alertTime;
	}
}

// ==========================================
// CONCURRENT DOMAIN ENTITY
// ==========================================

class Task {
	private final String taskId;
	private String title;
	private String description;
	private LocalDateTime dueDate;
	private Priority priority;
	private TaskStatus status;
	private User assignedUser;
	private final List<Reminder> reminders = new ArrayList<>();

	// Explicit mutual exclusion primitives per task instance
	private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

	public Task(String taskId, String title, String description, LocalDateTime dueDate, Priority priority) {
		this.taskId = taskId;
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.priority = priority;
		this.status = TaskStatus.PENDING;
	}

	public void lockRead() {
		rwLock.readLock().lock();
	}
	public void unlockRead() {
		rwLock.readLock().unlock();
	}
	public void lockWrite() {
		rwLock.writeLock().lock();
	}
	public void unlockWrite() {
		rwLock.writeLock().unlock();
	}

	public String getTaskId() {
		return taskId;
	}
	public String getTitle() {
		return title;
	}
	public Priority getPriority() {
		return priority;
	}
	public TaskStatus getStatus() {
		return status;
	}
	public User getAssignedUser() {
		return assignedUser;
	}
	public LocalDateTime getDueDate() {
		return dueDate;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}
	public void setAssignedUser(User user) {
		this.assignedUser = user;
	}
	public void addReminder(Reminder reminder) {
		this.reminders.add(reminder);
	}
}

// ==========================================
// EXTENSIBLE SPECIFICATION AND STRATEGY INTERFACES
// ==========================================

interface TaskSpecification {
	boolean isSatisfiedBy(Task task);
}

class PrioritySpecification implements TaskSpecification {
	private final Priority priority;
	public PrioritySpecification(Priority priority) {
		this.priority = priority;
	}
	@Override public boolean isSatisfiedBy(Task task) {
		return task.getPriority() == priority;
	}
}

class AssigneeSpecification implements TaskSpecification {
	private final String userId;
	public AssigneeSpecification(String userId) {
		this.userId = userId;
	}
	@Override public boolean isSatisfiedBy(Task task) {
		return task.getAssignedUser() != null && task.getAssignedUser().getUserId().equals(userId);
	}
}

interface TaskObserver {
	void onTaskChanged(Task task, String log);
}

class HistoryLogger implements TaskObserver {
	private final Map<String, List<TaskHistory>> internalLogs = new ConcurrentHashMap<>();

	@Override
	public void onTaskChanged(Task task, String log) {
		internalLogs.computeIfAbsent(task.getTaskId(), k -> new CopyOnWriteArrayList<>())
		.add(new TaskHistory(task.getTaskId(), log));
	}

	public void printHistory(String taskId) {
		System.out.println("\n--- History Trail for Task: " + taskId + " ---");
		internalLogs.getOrDefault(taskId, Collections.emptyList()).forEach(System.out::println);
	}
}

// ==========================================
// CORE SYSTEM SERVICE ORCHESTRATOR
// ==========================================

class TaskService {
	private final Map<String, Task> repository = new ConcurrentHashMap<>();
	private final List<TaskObserver> observers = new CopyOnWriteArrayList<>();

	public void registerObserver(TaskObserver observer) {
		observers.add(observer);
	}

	private void notifyObservers(Task task, String actionMessage) {
		observers.forEach(observer -> observer.onTaskChanged(task, actionMessage));
	}

	public void createTask(Task task) {
		repository.put(task.getTaskId(), task);
		notifyObservers(task, "Created Task: " + task.getTitle());
	}

	public void updateTaskStatus(String taskId, TaskStatus newStatus) {
		Task task = repository.get(taskId);
		if (task == null) return;

		task.lockWrite();
		try {
			TaskStatus oldStatus = task.getStatus();
			if (oldStatus != newStatus) {
				task.setStatus(newStatus);
				notifyObservers(task, "Status modified from " + oldStatus + " to " + newStatus);
			}
		} finally {
			task.unlockWrite();
		}
	}

	public void assignTask(String taskId, User user) {
		Task task = repository.get(taskId);
		if (task == null) return;

		task.lockWrite();
		try {
			task.setAssignedUser(user);
			notifyObservers(task, "Assigned to " + user.getName());
		} finally {
			task.unlockWrite();
		}
	}

	public void setReminder(String taskId, LocalDateTime time) {
		Task task = repository.get(taskId);
		if (task == null) return;

		task.lockWrite();
		try {
			task.addReminder(new Reminder(time));
			notifyObservers(task, "Configured task reminder for " + time);
		} finally {
			task.unlockWrite();
		}
	}

	public void deleteTask(String taskId) {
		Task removed = repository.remove(taskId);
		if (removed != null) {
			System.out.println("Task " + taskId + " safely purged from system registry.");
		}
	}

	public List<Task> queryTasks(List<TaskSpecification> filters) {
		return repository.values().stream()
		.filter(task -> {
			task.lockRead();
			try {
				return filters.stream().allMatch(f -> f.isSatisfiedBy(task));
			} finally {
				task.unlockRead();
			}
		})
		.collect(Collectors.toList());
	}
}

// ==========================================
// RUNTIME EXECUTION / ENTRYPOINT
// ==========================================

public class Main {
	public static void main(String[] args) {
		TaskService service = new TaskService();
		HistoryLogger logger = new HistoryLogger();
		service.registerObserver(logger);

		User dev = new User("U101", "Sachin");

		// Create Task
		Task task = new Task("T1", "Fix Multi-threading bugs", "Solve race conditions", LocalDateTime.now().plusDays(1), Priority.HIGH);
		service.createTask(task);

		// State Mutations
		service.assignTask("T1", dev);
		service.setReminder("T1", LocalDateTime.now().plusHours(2));
		service.updateTaskStatus("T1", TaskStatus.COMPLETED);

		// Specification Search Execution
		List<TaskSpecification> searchCriteria = List.of(
		            new PrioritySpecification(Priority.HIGH),
		            new AssigneeSpecification("U101")
		        );

		List<Task> results = service.queryTasks(searchCriteria);
		System.out.println("\nQuery Results Matched: " + results.size() + " task(s).");

		// Display History Log
		logger.printHistory("T1");

		// Deletion Hook
		service.deleteTask("T1");
	}
}