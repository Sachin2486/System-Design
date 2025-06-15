#include<bits/stdc++.h>
#include <iostream>

using namespace std;

enum class TaskStatus {
    Pending, InProgress, Completed
};

enum class TaskPriority {
    Low, Medium, High
};

string toString(TaskStatus status) {
    switch (status) {
        case TaskStatus::Pending: return "Pending";
        case TaskStatus::InProgress: return "In Progress";
        case TaskStatus::Completed: return "Completed";
    }
    return "";
}

struct User {
    string userId;
    string name;
    
    User(string id, string name) : userId(id), name(name) {}
};

struct Task {
    string taskId;
    string title;
    string description;
    time_t dueDate;
    TaskPriority priority;
    TaskStatus status;
    string assignedTo;
    time_t reminder;
    
    //default constructor 
    Task() 
        : taskId(""), title(""), description(""), dueDate(0),
          priority(TaskPriority::Low), status(TaskStatus::Pending),
          assignedTo(""), reminder(0) {}

    // parametrized constructor
    Task(string id, string t, string d, time_t due, TaskPriority p, const string& assignee)
        : taskId(id), title(t), description(d), dueDate(due), priority(p), status(TaskStatus::Pending), assignedTo(assignee) {
        reminder = dueDate - 3600; // default reminder: 1 hour before
    }
};

class TaskManager {
    private:
    unordered_map<string,Task> tasks;
    mutex mtx;
    int taskCounter = 1;
    
    string generateTaskId() {
        return "T" + to_string(taskCounter++);
    }
    
    public:
    string createTask(const string& title, const string& desc, time_t due,TaskPriority priority, const string& assignee) {
        lock_guard<mutex> lock(mtx);
        string id = generateTaskId();
        tasks[id] = Task(id, title, desc, due, priority, assignee);
        return id;
    }
    
    bool updateTask(const string& id, const string& title,const string& desc, time_t due, TaskPriority priority) {
        lock_guard<mutex> lock(mtx);
        if(tasks.find(id) == tasks.end()) return false;
        Task& task = tasks[id];
        task.title = title;
        task.description = desc;
        task.dueDate = due;
        task.priority = priority;
        return true;
    }
    
    bool deleteTask(const string& id) {
        lock_guard<mutex> lock(mtx);
        return tasks.erase(id) > 0;
    }
    
    bool assignTask(const string& id, const string& userId) {
        lock_guard<mutex> lock(mtx);
        if (tasks.find(id) == tasks.end()) return false;
        tasks[id].assignedTo = userId;
        return true;
    }
    
    bool markCompleted(const string& id) {
        lock_guard<mutex> lock(mtx);
        if (tasks.find(id) == tasks.end()) return false;
        tasks[id].status = TaskStatus::Completed;
        return true;
    }
    
    vector<Task> searchByPriority(TaskPriority priority) {
        lock_guard<mutex> lock(mtx);
        vector<Task> result;
        for (auto& [id, task] : tasks) {
            if (task.priority == priority)
                result.push_back(task);
        }
        return result;
    }
    
    vector<Task> searchByUser(const string& userId) {
        lock_guard<mutex> lock(mtx);
        vector<Task> result;
        for(auto& [id, task] : tasks) {
            if(task.assignedTo == userId)
            result.push_back(task);
        }
        return result;
    }
    
    void viewAllTasks() {
        lock_guard<mutex> lock(mtx);
        for (auto& [id, task] : tasks) {
            cout << "Task ID: " << task.taskId << " | " << task.title << " | " << toString(task.status)
                 << " | Priority: " << static_cast<int>(task.priority)
                 << " | Due: " << ctime(&task.dueDate);
        }
    }
};

int main() {
    TaskManager manager;
    time_t now = time(nullptr);
    time_t dueTomorrow = now + 86400;

    string t1 = manager.createTask("Submit Report", "Send the project report", dueTomorrow, TaskPriority::High, "u1");
    string t2 = manager.createTask("Team Meeting", "Weekly sync call", dueTomorrow, TaskPriority::Medium, "u2");

    manager.viewAllTasks();

    manager.markCompleted(t1);
    cout << "\nAfter Completion:\n";
    manager.viewAllTasks();

    cout << "\nTasks for user u1:\n";
    for (auto& t : manager.searchByUser("u1")) {
        cout << "- " << t.title << " | " << toString(t.status) << endl;
    }

    return 0;
}
