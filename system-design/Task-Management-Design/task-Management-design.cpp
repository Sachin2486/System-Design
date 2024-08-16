#include <iostream>
#include <string>
#include <vector>
#include <algorithm>
#include <ctime>
#include <iomanip>
#include <sstream>

using namespace std;

enum class Priority {
    Low,
    Medium,
    High
};  // Missing semicolon fixed

enum class Status {
    Pending,
    InProgress,
    Completed
};  // Missing semicolon fixed

struct Task {
    int id;
    string title;
    string description;
    time_t dueDate;
    Priority priority;
    Status status;
    string assignedUser;
    bool reminderSet;

    Task(int id_, const string &title_, const string &desc_, time_t dueDate_,
         Priority priority_, const string &assignedUser_) 
         : id(id_), title(title_), description(desc_), dueDate(dueDate_), priority(priority_), 
           status(Status::Pending), assignedUser(assignedUser_), reminderSet(false) {}
};

class TaskManager {
private:
    vector<Task> tasks;
    int taskCounter;

public:
    TaskManager() : taskCounter(0) {}

    // Create a new task
    void createTask(const string &title, const string &description, time_t dueDate,
                    Priority priority, const string &assignedUser) {
        tasks.push_back(Task(++taskCounter, title, description, dueDate, priority, assignedUser));
        cout << "Task '" << title << "' created successfully!\n";
    }

    // Update an existing task
    void updateTask(int taskId, const string &title, const string &description, time_t dueDate,
                    Priority priority, const string &assignedUser, Status status, bool reminderSet) {
        for (auto &task : tasks) {
            if (task.id == taskId) {
                task.title = title;
                task.description = description;
                task.dueDate = dueDate;
                task.priority = priority;
                task.assignedUser = assignedUser;
                task.status = status;
                task.reminderSet = reminderSet;
                cout << "Task '" << title << "' updated successfully!\n";
                return;
            }
        }
        cout << "Task not found!\n";
    }

    // Delete a task
    void deleteTask(int taskId) {
        auto it = remove_if(tasks.begin(), tasks.end(), [&](const Task &task) {
            return task.id == taskId;
        });
        if (it != tasks.end()) {
            cout << "Task '" << it->title << "' deleted successfully!\n";
            tasks.erase(it, tasks.end());  // Fix: erase the removed elements
        } else {
            cout << "Task not found!\n";
        }
    }

    // Mark a task as completed
    void markTaskAsCompleted(int taskId) {
        for (auto &task : tasks) {
            if (task.id == taskId) {
                task.status = Status::Completed;
                cout << "Task '" << task.title << "' marked as completed!\n";
                return;
            }
        }
        cout << "Task not found!\n";
    }

    // View task history
    void viewTaskHistory() const {
        for (const auto &task : tasks) {
            if (task.status == Status::Completed) {
                cout << "Task ID: " << task.id << ", Title: " << task.title << ", Completed!\n";
            }
        }
    }

    // Search and filter tasks based on various criteria
    void searchAndFilterTasks(Priority priority = Priority::Low, const string &assignedUser = "",
                              Status status = Status::Pending) const {
        for (const auto &task : tasks) {
            if ((task.priority == priority || priority == Priority::Low) &&
                (task.assignedUser == assignedUser || assignedUser.empty()) &&
                (task.status == status || status == Status::Pending)) {
                cout << "Task ID: " << task.id << ", Title: " << task.title
                     << ", Due Date: " << ctime(&task.dueDate) << ", Status: "
                     << (task.status == Status::Pending ? "Pending" : 
                         (task.status == Status::InProgress ? "In Progress" : "Completed")) << "\n";
            }
        }
    }
};

// Utility function to convert string to time_t
time_t stringToTime(const string &dateStr) {
    tm tm = {};
    istringstream ss(dateStr);
    ss >> get_time(&tm, "%Y-%m-%d");
    return mktime(&tm);
}

int main() {
    TaskManager manager;

    // Example to create and manage tasks
    manager.createTask("Task 1", "Complete project", stringToTime("2024-08-20"), Priority::High, "User1");
    manager.createTask("Task 2", "Write report", stringToTime("2024-08-18"), Priority::Medium, "User2");

    // Update a task
    manager.updateTask(1, "Task 1 Updated", "Complete project with changes", stringToTime("2024-08-21"),
                       Priority::High, "User1", Status::InProgress, true);

    // Search and filter tasks
    cout << "\nFiltered tasks by priority and user:\n";
    manager.searchAndFilterTasks(Priority::High, "User1", Status::InProgress);

    // Mark a task as completed
    manager.markTaskAsCompleted(2);

    // View task history
    cout << "\nCompleted tasks:\n";
    manager.viewTaskHistory();

    // Delete a task
    manager.deleteTask(1);

    return 0;
}
