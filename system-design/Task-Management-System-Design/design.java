import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

enum TaskStatus {
	PENDING,
	IN_PROGRESS,
	COMPLETED
}

enum TaskPriority {
	LOW,
	MEDIUM,
	HIGH
}

// Represents a user in the system
// Helps to meet requirement: "Users should be able to assign tasks to other users"
class User {
	private final String UserID;
	private String name;
	private String email;

	public User(String UserID, String name, String email) {
		this.UserID = UserID;
		this.name = name;
		this.email = email;
	}

	public String getUserID() {
		return UserID;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}
}

// Represents a task in the system
// Helps to meet requirement: "Each task should have title, description, due date, priority, status"
class Task {
	private final String taskId;
	private String title;
	private String description;
	private LocalDate dueDate;
	private TaskPriority priority;
	private TaskStatus status;
	private User assignedUser;
	private LocalDate reminderDate;

	public Task(String taskId, String title, String description, LocalDate dueDate, TaskPriority priority) {
		this.taskId = taskId;
		this.title = title;
		this.description = description;
		this.dueDate = dueDate;
		this.priority = priority;
		this.status = TaskStatus.PENDING;
	}

	public String getTaskId() {
		return taskId;
	}
	public String getTitle() {
		return title;
	}
	public String getDescription() {
		return description;
	}
	public LocalDate getDueDate() {
		return dueDate;
	}
	public TaskPriority getPriority() {
		return priority;
	}
	public TaskStatus getStatus() {
		return status;
	}
	public User getAssignedUser() {
		return assignedUser;
	}
	public LocalDate getReminderDate() {
		return reminderDate;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}
	public void setPriority(TaskPriority priority) {
		this.priority = priority;
	}
	public void setStatus(TaskStatus status) {
		this.status = status;
	}
	public void setAssignedUser(User user) {
		this.assignedUser = user;
	}
	public void setReminderDate(LocalDate reminderDate) {
		this.reminderDate = reminderDate;
	}
}

// Main service/facade class to manage tasks
// Helps to meet requirements: create/update/delete/search/filter/mark tasks & handle concurrency
class TaskManager {
	private final Map<String, Task> tasks = new ConcurrentHashMap<>();
	private final ReentrantLock lock = new ReentrantLock();

	// Create a new task
	public Task createTask(String title, String desc, LocalDate dueDate, TaskPriority priority) {
		lock.lock();
		try {
			String id = UUID.randomUUID().toString();
			Task task = new Task(id,title,desc,dueDate,priority);
			tasks.put(id,task);
			return task;
		} finally {
			lock.unlock();
		}
	}

	// Update an existing task
	public boolean updateTask(String taskId, String title,String desc,LocalDate dueDate, TaskPriority priority) {
		lock.lock();
		try {
			Task task = tasks.get(taskId);
			if(task == null) return false;

			if(title != null) task.setTitle(title);
			if(desc != null) task.setDescription(desc);
			if(dueDate != null) task.setDueDate(dueDate);
			if(priority != null) task.setPriority(priority);
			return true;
		} finally {
			lock.unlock();
		}
	}

	// Delete a task
	public boolean deleteTask(String taskId) {
		lock.lock();
		try {
			return tasks.remove(taskId) != null;
		} finally {
			lock.unlock();
		}
	}

	// Assign task to a user
	public boolean assignTask(String taskId, User user) {
		lock.lock();
		try {
			Task task = tasks.get(taskId);
			if (task == null) return false;
			task.setAssignedUser(user);
			return true;
		} finally {
			lock.unlock();
		}
	}

	// Mark task as completed
	public boolean markCompleted(String taskId) {
		lock.lock();
		try {
			Task task = tasks.get(taskId);
			if (task == null) return false;
			task.setStatus(TaskStatus.COMPLETED);
			return true;
		} finally {
			lock.unlock();
		}
	}

	public boolean setReminder(String taskId, LocalDate reminderDate) {
		lock.lock();
		try {
			Task task = tasks.get(taskId);
			if (task == null) return false;
			task.setReminderDate(reminderDate);
			return true;
		} finally {
			lock.unlock();
		}
	}

	// Search/filter tasks by criteria
	public List<Task> searchTasks(TaskPriority priority, TaskStatus status, User assignedUser) {
		List<Task> result = new ArrayList<>();
		for (Task task : tasks.values()) {
			if ((priority == null || task.getPriority() == priority) &&
			        (status == null || task.getStatus() == status) &&
			        (assignedUser == null || task.getAssignedUser() == assignedUser)) {
				result.add(task);
			}
		}
		return result;
	}

	// Get task history for a user (all completed tasks)
	public List<Task> getCompletedTasksByUser(User user) {
		List<Task> result = new ArrayList<>();
		for (Task task : tasks.values()) {
			if (task.getAssignedUser() == user && task.getStatus() == TaskStatus.COMPLETED) {
				result.add(task);
			}
		}
		return result;
	}
}


// Demo class to test task management system
public class Demo {
    public static void main (String[] args) {
        TaskManager manager = new TaskManager();
        User sachin = new User("U1", "Sachin", "sachin@example.com");
        User alex = new User("U2", "Alex", "alex@example.com");
        
        Task t1 = manager.createTask("Finish report", "Complete by Monday", LocalDate.now().plusDays(3), TaskPriority.HIGH);
        Task t2 = manager.createTask("Buy groceries", "Milk, Bread, Eggs", LocalDate.now().plusDays(1), TaskPriority.MEDIUM);

        manager.assignTask(t1.getTaskId(), sachin);
        manager.assignTask(t2.getTaskId(), alex);

        manager.setReminder(t1.getTaskId(), LocalDate.now().plusDays(2));
        manager.markCompleted(t1.getTaskId());
        
        System.out.println("Completed tasks for Sachin: " + manager.getCompletedTasksByUser(sachin).size());
        System.out.println("Tasks with HIGH priority: " + manager.searchTasks(TaskPriority.HIGH, null, null).size());
    }
}
