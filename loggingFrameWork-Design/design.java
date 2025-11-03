import java.sql.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// ---------------------- LogLevel Enum ----------------------
enum LogLevel {
    DEBUG(1),
    INFO(2),
    WARNING(3),
    ERROR(4),
    FATAL(5);

    private final int levelValue;
    LogLevel(int value) {
        this.levelValue = value;
    }
    public int getValue() {
        return levelValue;
    }
}

// ---------------------- LogMessage ----------------------
class LogMessage {
    private final String timestamp;
    private final LogLevel level;
    private final String message;

    public LogMessage(LogLevel level, String message) {
        this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.level = level;
        this.message = message;
    }

    public String format() {
        return String.format("[%s] [%s] %s", timestamp, level, message);
    }

    public LogLevel getLevel() {
        return level;
    }
}

// ---------------------- Appender Interface ----------------------
interface Appender {
    void append(LogMessage logMessage) throws Exception;
}

// ---------------------- ConsoleAppender ----------------------
class ConsoleAppender implements Appender {
    @Override
    public void append(LogMessage logMessage) {
        System.out.println(logMessage.format());
    }
}

// ---------------------- FileAppender ----------------------
class FileAppender implements Appender {
    private final String filePath;

    public FileAppender(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public synchronized void append(LogMessage logMessage) throws IOException {
        try (FileWriter fw = new FileWriter(filePath, true)) {
            fw.write(logMessage.format() + "\n");
        }
    }
}

// ---------------------- DatabaseAppender (Mock) ----------------------
class DatabaseAppender implements Appender {
    @Override
    public void append(LogMessage logMessage) throws SQLException {
        // In real-world, you’d use JDBC here.
        // For simplicity, we simulate DB logging:
        System.out.println("DB Log => " + logMessage.format());
    }
}

// ---------------------- LoggerConfig ----------------------
class LoggerConfig {
    private LogLevel currentLevel;
    private final List<Appender> appenders;

    public LoggerConfig(LogLevel level) {
        this.currentLevel = level;
        this.appenders = Collections.synchronizedList(new ArrayList<>());
    }

    public void setLevel(LogLevel level) {
        this.currentLevel = level;
    }

    public LogLevel getLevel() {
        return currentLevel;
    }

    public void addAppender(Appender appender) {
        appenders.add(appender);
    }

    public List<Appender> getAppenders() {
        return appenders;
    }
}

// ---------------------- Logger ----------------------
class Logger {
    private final LoggerConfig config;

    public Logger(LoggerConfig config) {
        this.config = config;
    }

    public void log(LogLevel level, String message) {
        if (level.getValue() >= config.getLevel().getValue()) {
            LogMessage logMessage = new LogMessage(level, message);
            for (Appender appender : config.getAppenders()) {
                try {
                    appender.append(logMessage);
                } catch (Exception e) {
                    System.err.println("Failed to log message: " + e.getMessage());
                }
            }
        }
    }

    // Helper methods for convenience
    public void debug(String msg) { log(LogLevel.DEBUG, msg); }
    public void info(String msg) { log(LogLevel.INFO, msg); }
    public void warn(String msg) { log(LogLevel.WARNING, msg); }
    public void error(String msg) { log(LogLevel.ERROR, msg); }
    public void fatal(String msg) { log(LogLevel.FATAL, msg); }
}

// ---------------------- Demo ----------------------
public class LoggingFrameworkDemo {
    public static void main(String[] args) {
        // Step 1: Configure Logger
        LoggerConfig config = new LoggerConfig(LogLevel.DEBUG);
        config.addAppender(new ConsoleAppender());
        config.addAppender(new FileAppender("application.log"));
        config.addAppender(new DatabaseAppender());

        // Step 2: Create Logger
        Logger logger = new Logger(config);

        // Step 3: Use Logger
        logger.debug("Debugging application startup...");
        logger.info("Application started successfully.");
        logger.warn("Memory usage is high.");
        logger.error("Database connection failed.");
        logger.fatal("System crash!");

        // Step 4: Change Log Level dynamically
        config.setLevel(LogLevel.ERROR);
        logger.info("This will NOT be printed because log level is ERROR.");
        logger.error("This WILL be printed.");
    }
}
