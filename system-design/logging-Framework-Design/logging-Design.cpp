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

class LogDestination {
public:
	virtual void log(const std::string& message) = 0;
	virtual ~LogDestination() = default;
};

class ConsoleLogDestination : public LogDestination {
public:
	void log(const string& message) override {
		cout<<message<<endl;
	}
};

class FileLogDestination : public LogDestination {
private:
	std::ofstream logFile;
public:
	explicit FileLogDestination(const std::string& filename) {
		logFile.open(filename, std::ios::app);
	}

	void log(const string& message) override {
		if (logFile.is_open()) {
			logFile << message << std::endl;
		}
	}

	~FileLogDestination() {
		if (logFile.is_open()) {
			logFile.close();
		}
	}
};

class DatabaseLogDestination : public LogDestination {
public:
	void log(const std::string& message) override {
		// Connect to database and log the message
		std::cout << "Database Log: " << message << std::endl; // Placeholder
	}
};

class Logger {
private:
	LogLevel currentLogLevel;
	std::map<LogLevel,string> logLevelStrings = {
		{LogLevel::DEBUG, "DEBUG"},
		{LogLevel::INFO, "INFO"},
		{LogLevel::WARNING, "WARNING"},
		{LogLevel::ERROR, "ERROR"},
		{LogLevel::FATAL, "FATAL"}
	};
	std::vector<std::shared_ptr<LogDestination>> destinations;
	std::mutex logMutex;

	std::string getTimestamp() const {
		auto now = std::chrono::system_clock::now();
		std::time_t now_time = std::chrono::system_clock::to_time_t(now);
		std::tm now_tm = *std::localtime(&now_time);
		std::ostringstream oss;
		oss << std::put_time(&now_tm, "%Y-%m-%d %H:%M:%S");
		return oss.str();
	}

	std::string formatMessage(LogLevel level, const std::string& message) const {
		std::ostringstream oss;
		oss << "[" << getTimestamp() << "] "
		    << "[" << logLevelStrings.at(level) << "] "
		    << message;
		return oss.str();
	}

public:
	explicit Logger(LogLevel level = LogLevel::INFO)
		: currentLogLevel(level) {}

	void addDestination(const std::shared_ptr<LogDestination>& destination) {
		std::lock_guard<std::mutex> lock(logMutex);
		destinations.push_back(destination);
	}

	void setLogLevel(LogLevel level) {
		std::lock_guard<std::mutex> lock(logMutex);
		currentLogLevel = level;
	}

	void log(LogLevel level, const std::string& message) {
		std::lock_guard<std::mutex> lock(logMutex);
		if (level >= currentLogLevel) {
			std::string formattedMessage = formatMessage(level, message);
			for (const auto& destination : destinations) {
				destination->log(formattedMessage);
			}
		}
	}

	void debug(const std::string& message) {
		log(LogLevel::DEBUG, message);
	}

	void info(const std::string& message) {
		log(LogLevel::INFO, message);
	}

	void warning(const std::string& message) {
		log(LogLevel::WARNING, message);
	}

	void error(const std::string& message) {
		log(LogLevel::ERROR, message);
	}

	void fatal(const std::string& message) {
		log(LogLevel::FATAL, message);
	}
};

int main()
{
	Logger logger(LogLevel::DEBUG);

	// Added the destinations
	logger.addDestination(std::make_shared<ConsoleLogDestination>());
	logger.addDestination(std::make_shared<FileLogDestination>("logfile.log"));
	logger.addDestination(std::make_shared<DatabaseLogDestination>());

	// Log the messages
	logger.debug("This is a debug message.");
	logger.info("This is an info message.");
	logger.warning("This is a warning message.");
	logger.error("This is an error message.");
	logger.fatal("This is a fatal message.");

	return 0;
}