#include <bits/stdc++.h>
#include <iostream>

using namespace std;

enum class LogLevel {
	DEBUG,
	INFO,
	WARNING,
	ERROR,
	FATAL
};

string LogLevelToString (LogLevel level) {
    switch(level) {
        case LogLevel::DEBUG: return "DEBUG";
        case LogLevel::INFO: return "INFO";
        case LogLevel::WARNING: return "WARNING";
        case LogLevel::ERROR: return "ERROR";
        case LogLevel::FATAL: return "FATAL";
    }
    return "UNKNOWN";
};

class ILogDestination {
public:
    virtual void logMessage(const string& timestamp, LogLevel level, const string& message) = 0;
    virtual ~ILogDestination() = default;
};

class ConsoleLogger : public ILogDestination {
public:
    void logMessage(const string& timestamp, LogLevel level, const string& message) override {
        cout << "[" << timestamp << "] [" << LogLevelToString(level) << "] " << message << endl;
    }
};

class FileLogger : public ILogDestination {
    private:
    ofstream file;
    mutex fileMutex;
    
    public:
    FileLogger(const string& filename) {
        file.open(filename, ios::app);
    }
    
    void logMessage(const string& timestamp, LogLevel level, const string& message) override {
        lock_guard<mutex> lock(fileMutex);
        file << "[" << timestamp << "] [" << LogLevelToString(level) << "] " << message << endl;
    }
    
    ~FileLogger() {
        if (file.is_open()) file.close();
    }
};

class DatabaseLogger : public ILogDestination {
public:
    void logMessage(const string& timestamp, LogLevel level, const string& message) override {
        // Simulate DB insertion
        cout << "[DB] [" << timestamp << "] [" << LogLevelToString(level) << "] " << message << endl;
    }
};

class LoggerConfig {
public:
    LogLevel minLevel;
    vector<shared_ptr<ILogDestination>> destinations;

    LoggerConfig(LogLevel level = LogLevel::INFO) : minLevel(level) {}

    void addDestination(shared_ptr<ILogDestination> dest) {
        destinations.push_back(dest);
    }
};

class Logger {
    private:
    LoggerConfig config;
    mutex logMutex;
    
    string getCurrentTimestamp() {
        time_t now = time(nullptr);
        char buf[100];
        strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", localtime(&now));
        return string(buf);
    }
    
    public:
    Logger(const LoggerConfig& cfg) : config(cfg) {}
    
    void log(LogLevel level, const string& message) {
        if (level < config.minLevel) return;
        
        string timestamp = getCurrentTimestamp();
        
        lock_guard<mutex> lock(logMutex);
        for (auto& dest : config.destinations) {
            dest->logMessage(timestamp, level, message);
        }
    }
    
    void debug(const string& msg)   { log(LogLevel::DEBUG, msg); }
    void info(const string& msg)    { log(LogLevel::INFO, msg); }
    void warning(const string& msg) { log(LogLevel::WARNING, msg); }
    void error(const string& msg)   { log(LogLevel::ERROR, msg); }
    void fatal(const string& msg)   { log(LogLevel::FATAL, msg); }
};

int main()

{
	LoggerConfig config(LogLevel::DEBUG); // log everything >= DEBUG
    config.addDestination(make_shared<ConsoleLogger>());
    config.addDestination(make_shared<FileLogger>("logfile.txt"));
    config.addDestination(make_shared<DatabaseLogger>());

    Logger logger(config);

    logger.debug("This is a debug message");
    logger.info("User logged in");
    logger.warning("Disk space running low");
    logger.error("Unable to connect to server");
    logger.fatal("System crash!");

    return 0;
}