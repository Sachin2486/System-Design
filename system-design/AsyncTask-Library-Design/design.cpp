#include <iostream>
#include <functional>
#include <vector>
#include <atomic>
#include <mutex>
#include <memory>
#include <queue>
#include <unordered_map>
#include <thread>
#include <condition_variable>
#include <chrono>

class Task {
public:
    using Ptr = std::shared_ptr<Task>;

    Task(int id, std::function<void()> func)
        : id(id), taskFunc(func), completed(false) {}

    void addDependency(Ptr dependency) {
        std::lock_guard<std::mutex> lock(depMutex);
        dependencies.push_back(dependency);
    }

    bool isReady() {
        std::lock_guard<std::mutex> lock(depMutex);
        for (auto& dep : dependencies) {
            if (!dep->isCompleted()) return false;
        }
        return true;
    }

    void execute() {
        taskFunc();
        completed = true;
    }

    bool isCompleted() const {
        return completed;
    }

    int getId() const {
        return id;
    }

private:
    int id;
    std::function<void()> taskFunc;
    std::atomic<bool> completed;
    std::vector<Ptr> dependencies;
    std::mutex depMutex;
};

class TaskManager {
public:
    static TaskManager& getInstance() {
        static TaskManager instance;
        return instance;
    }

    void addTask(Task::Ptr task) {
        {
            std::lock_guard<std::mutex> lock(queueMutex);
            tasks[task->getId()] = task;
            taskQueue.push(task);
        }
        queueCV.notify_one();
    }

    void runTasks() {
        std::thread([this]() {
            while (true) {
                Task::Ptr task = nullptr;

                {
                    std::unique_lock<std::mutex> lock(queueMutex);
                    queueCV.wait(lock, [this]() { return !taskQueue.empty(); });

                    size_t size = taskQueue.size();
                    for (size_t i = 0; i < size; ++i) {
                        auto t = taskQueue.front(); taskQueue.pop();
                        if (t->isReady() && !t->isCompleted()) {
                            task = t;
                            break;
                        } else {
                            taskQueue.push(t);
                        }
                    }
                }

                if (task != nullptr) {
                    std::thread([task]() {
                        task->execute();
                    }).detach();
                } else {
                    std::this_thread::sleep_for(std::chrono::milliseconds(10));
                }
            }
        }).detach();
    }

private:
    TaskManager() {}
    std::queue<Task::Ptr> taskQueue;
    std::unordered_map<int, Task::Ptr> tasks;
    std::mutex queueMutex;
    std::condition_variable queueCV;
};

int main() {
    auto& manager = TaskManager::getInstance();

    auto task1 = std::make_shared<Task>(1, [] {
        std::cout << "Task 1 started\n";
        std::this_thread::sleep_for(std::chrono::seconds(1));
        std::cout << "Task 1 done\n";
    });

    auto task2 = std::make_shared<Task>(2, [] {
        std::cout << "Task 2 started\n";
        std::this_thread::sleep_for(std::chrono::seconds(1));
        std::cout << "Task 2 done\n";
    });

    auto task3 = std::make_shared<Task>(3, [] {
        std::cout << "Task 3 started (depends on Task 1 & 2)\n";
        std::this_thread::sleep_for(std::chrono::seconds(1));
        std::cout << "Task 3 done\n";
    });

    task3->addDependency(task1);
    task3->addDependency(task2);

    manager.addTask(task1);
    manager.addTask(task2);
    manager.addTask(task3);

    manager.runTasks();

    std::this_thread::sleep_for(std::chrono::seconds(5)); // Wait for tasks to finish
    return 0;
}
