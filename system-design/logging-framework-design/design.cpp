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

struct LogMessage {
	LogLevel level;
	std::string message;
	std::chrono::system_clock::time_point timestamp;
};

class Formatter {
public:
	static std:: string format(const LogMessage& logMsg) {
		std::ostringstream oss;

		auto t = std::chrono::system_clock::to_time_t(logMsg.timestamp);
		oss << "[" << std::put_time(std::localtime(&t), "%Y-%m-%d %H:%M:%S") << "]";
		oss << "[" << logLevelToString(logMsg.level) << "]";
		oss << " " << logMsg.message;
		return oss.str();
	}

private:

	static std::string logLevelToString(LogLevel level) {
		switch(level) {
		case LogLevel::DEBUG:
			return "DEBUG";
		case LogLevel::INFO:
			return "INFO";
		case LogLevel::WARNING:
			return "WARNING";
		case LogLevel::FATAL:
			return "FATAL";
		default:
			return "UNKNOWN";
		}
	}
};

class ILogDestination {
public:
	virtual void log(const std::string& formattedMessage) = 0;
	virtual ~ILogDestination() = default;
};

class ConsoleDestination : public ILogDestination {
public:
	void log(const std::string& formattedMessage) override {
		std::cout << formattedMessage << std::endl;
	}
};

class FileDestination : public ILogDestination {
private:
	std::ofstream outFile;
public:
	FileDestination(const std::string& filename) {
		outFile.open(filename, std::ios::app);
	}

	void log(const std::string& formattedMessage) override {
		if (outFile.is_open()) {
			outFile << formattedMessage << std::endl;
		}
	}

	~FileDestination() {
		if (outFile.is_open()) outFile.close();
	}
};

class LoggerConfig {
public:
	LogLevel minLogLevel = LogLevel::DEBUG;
	std::vector<std::shared_ptr<ILogDestination>> destinations;

	void addDestination(std::shared_ptr<ILogDestination> dest) {
		destinations.push_back(dest);
	}

	void setMinLogLevel(LogLevel level) {
		minLogLevel = level;
	}
};

class Logger {
private:
	LoggerConfig config;
	std::mutex logMutex;

public:
	Logger(const LoggerConfig& cfg) : config(cfg) {}

	void log(LogLevel level, const std::string& message) {
		if (level < config.minLogLevel) return;

		std::lock_guard<std::mutex> lock(logMutex);

		LogMessage logMsg{level, message, std::chrono::system_clock::now()};
		std::string formatted = Formatter::format(logMsg);

		for (auto& dest : config.destinations) {
			dest->log(formatted);
		}
	}
};

void logMessages(Logger& logger, int id) {
	for (int i = 0; i < 5; ++i) {
		logger.log(LogLevel::INFO, "Thread " + std::to_string(id) + " - message " + std::to_string(i));
	}
}

int main()
{
	LoggerConfig config;
	config.setMinLogLevel(LogLevel::INFO);
	config.addDestination(std::make_shared<ConsoleDestination>());
	config.addDestination(std::make_shared<FileDestination>("logfile.txt"));

	Logger logger(config);

	logger.log(LogLevel::INFO, "System initialized");
	logger.log(LogLevel::DEBUG, "This won't be shown due to log level config");
	logger.log(LogLevel::ERROR, "Error connecting to database");

	std::thread t1(logMessages, std::ref(logger), 1);
	std::thread t2(logMessages, std::ref(logger), 2);
	t1.join();
	t2.join();

	return 0;
}