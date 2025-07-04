#include <bits/stdc++.h>
using namespace std;

enum class TaskType {
    STORY, FEATURE, BUG
};

enum class Status {
    TODO, IN_PROGRESS, DONE
};

string to_string(TaskType type) {
    switch (type) {
        case TaskType::STORY: return "Story";
        case TaskType::FEATURE: return "Feature";
        case TaskType::BUG: return "Bug";
    }
    return "";
}

string to_string(Status s) {
    switch (s) {
        case Status::TODO: return "TODO";
        case Status::IN_PROGRESS: return "IN_PROGRESS";
        case Status::DONE: return "DONE";
    }
    return "";
}

class Task {
protected:
    int id;
    string title;
    string assignee;
    TaskType type;
    Status status;
    time_t dueDate;

    Task(int id, const string& title, TaskType type, const string& assignee, time_t dueDate)
        : id(id), title(title), assignee(assignee), type(type), dueDate(dueDate), status(Status::TODO) {}

    virtual void print() const {
        cout << "Task ID: " << id << ", Title: " << title << ", Type: " << to_string(type)
             << ", Status: " << to_string(status) << ", Assignee: " << assignee
             << ", Due: " << ctime(&dueDate);
    }

    virtual ~Task() {}

    friend class TaskManager;
    friend class SprintManager;
};

class Story : public Task {
public:
    vector<int> subtaskIds;

    Story(int id, const string& title, const string& assignee, time_t dueDate)
        : Task(id, title, TaskType::STORY, assignee, dueDate) {}

    void addSubtask(int taskId) {
        subtaskIds.push_back(taskId);
    }

    void print() const override {
        Task::print();
        cout << "Subtasks: ";
        for (int subId : subtaskIds) cout << subId << " ";
        cout << endl;
    }
};

class Sprint {
public:
    string name;
    set<int> taskIds;

    Sprint() = default;
    Sprint(const string& name) : name(name) {}

    void addTask(int taskId) {
        taskIds.insert(taskId);
    }

    void removeTask(int taskId) {
        taskIds.erase(taskId);
    }

    void print() const {
        cout << "Sprint: " << name << ", Tasks: ";
        for (int tid : taskIds)
            cout << tid << " ";
        cout << endl;
    }
};

class TaskManager {
private:
    int nextId = 1;
    unordered_map<int, Task*> tasks;

public:
    ~TaskManager() {
        for (auto& [id, task] : tasks) delete task;
    }

    int createTask(const string& title, TaskType type, const string& assignee, time_t dueDate) {
        Task* task;
        if (type == TaskType::STORY)
            task = new Story(nextId, title, assignee, dueDate);
        else
            task = new Task(nextId, title, type, assignee, dueDate);
        tasks[nextId] = task;
        return nextId++;
    }

    void addSubtask(int storyId, int subtaskId) {
        if (tasks.count(storyId) && tasks.count(subtaskId)) {
            auto* story = dynamic_cast<Story*>(tasks[storyId]);
            if (story)
                story->addSubtask(subtaskId);
            else
                cout << "Error: Task " << storyId << " is not a story.\n";
        }
    }

    void updateStatus(int taskId, Status newStatus) {
        if (tasks.count(taskId))
            tasks[taskId]->status = newStatus;
    }

    void printDelayedTasks() const {
        time_t now = time(nullptr);
        cout << "Delayed Tasks:\n";
        for (const auto& [id, task] : tasks) {
            if (task->dueDate < now && task->status != Status::DONE)
                task->print();
        }
    }

    void printTasksForUser(const string& user) const {
        cout << "Tasks assigned to " << user << ":\n";
        for (const auto& [id, task] : tasks) {
            if (task->assignee == user)
                task->print();
        }
    }

    Task* getTask(int id) const {
        auto it = tasks.find(id);
        return (it != tasks.end()) ? it->second : nullptr;
    }
};

class SprintManager {
private:
    unordered_map<string, Sprint> sprints;

public:
    void createSprint(const string& name) {
        sprints[name] = Sprint(name);
    }

    void addTaskToSprint(const string& sprintName, int taskId) {
        sprints[sprintName].addTask(taskId);
    }

    void removeTaskFromSprint(const string& sprintName, int taskId) {
        sprints[sprintName].removeTask(taskId);
    }

    void printSprintDetails(const string& sprintName, const TaskManager& taskMgr) const {
        auto it = sprints.find(sprintName);
        if (it == sprints.end()) {
            cout << "Sprint not found.\n";
            return;
        }

        const Sprint& sprint = it->second;
        sprint.print();
        for (int tid : sprint.taskIds) {
            Task* task = taskMgr.getTask(tid);
            if (task)
                task->print();
        }
    }
};

int main() {
    TaskManager taskMgr;
    SprintManager sprintMgr;

    time_t now = time(nullptr);
    time_t yesterday = now - 86400; // 1 day ago
    time_t tomorrow = now + 86400;

    int bug1 = taskMgr.createTask("Fix Login Bug", TaskType::BUG, "Alice", yesterday);
    int feat1 = taskMgr.createTask("Add Dark Mode", TaskType::FEATURE, "Bob", tomorrow);
    int story1 = taskMgr.createTask("User Onboarding", TaskType::STORY, "Charlie", tomorrow);
    int subtask1 = taskMgr.createTask("Create Welcome Email", TaskType::FEATURE, "Charlie", tomorrow);

    taskMgr.addSubtask(story1, subtask1);
    taskMgr.updateStatus(bug1, Status::IN_PROGRESS);

    sprintMgr.createSprint("Sprint 1");
    sprintMgr.addTaskToSprint("Sprint 1", bug1);
    sprintMgr.addTaskToSprint("Sprint 1", feat1);
    sprintMgr.addTaskToSprint("Sprint 1", story1);

    cout << "\n--- Sprint Details ---\n";
    sprintMgr.printSprintDetails("Sprint 1", taskMgr);

    cout << "\n--- Delayed Tasks ---\n";
    taskMgr.printDelayedTasks();

    cout << "\n--- Tasks for Charlie ---\n";
    taskMgr.printTasksForUser("Charlie");

    return 0;
}
