// LogLevel (enum) → defines DEBUG, INFO, WARNING, ERROR, FATAL.
// LogMessage → stores timestamp, level, and content.
// Appender (interface) → defines how/where to write logs (console, file, database).
//     a) ConsoleAppender → logs to console.
//     b) FileAppender → logs to file.
//     c) DatabaseAppender → logs to database (we’ll mock with print)
// Logger → main facade class to log messages, manage appenders, and set log levels.
// LoggerConfig → configuration (log level, appenders).

import java.io.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.*;


enum LogLevel {
    DEBUG(1), INFO(2), WARNING(3), ERROR(4), FATAL(5);
    
    private int priority;
    LogLevel(int priority) {
        this.priority = priority;
    }
    
    public int getPriority() {
        return priority;
    }
}

class LogMessage {
    private Date timestamp;
    private LogLevel level;
    private String message;
    
    public LogMessage(LogLevel level, String message) {
        this.timestamp = new Date();
        this.level = level;
        this.message = message;
    }
    
    public String format() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "[" + sdf.format(timestamp) + "] [" + level + "] " + message;
    }
    
    public LogLevel getLevel() { 
        return level;
    }
}

interface Appender {
    void append(LogMessage log);
}

// --- ConsoleAppender ---
class ConsoleAppender implements Appender {
    public void append(LogMessage log) {
        System.out.println(log.format());
    }
}

// --- FileAppender ---
class FileAppender implements Appender {
    private String filePath;

    public FileAppender(String filePath) {
        this.filePath = filePath;
    }

    public synchronized void append(LogMessage log) {
        try (FileWriter fw = new FileWriter(filePath, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(log.format());
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// --- DatabaseAppender (Mock) ---
class DatabaseAppender implements Appender {
    public void append(LogMessage log) {
        // In real-world, we will insert it into DB
        System.out.println("[DB] " + log.format());
    }
}

class LoggerConfig {
    private LogLevel currentLevel;
    private List<Appender> appenders;
    
    public LoggerConfig(LogLevel currentLevel) {
        this.currentLevel = currentLevel;
        this.appenders = new ArrayList<>();
    }
    
    public LogLevel getCurrentLevel() {
        return currentLevel;
    }
    
    public void setCurrentLevel(LogLevel level) {
        this.currentLevel = level;
    }
    
    public void addAppender(Appender appender) {
        appenders.add(appender);
    }
    
    public List<Appender> getAppenders() {
        return appenders;
    }
}

// --- Logger (Facade) ---
class Logger {
    private LoggerConfig config;
    private final Lock lock = new ReentrantLock(); // For Thread-safety
    
    public Logger(LoggerConfig config) {
        this.config = config;
    }
    
    public void log(LogLevel level, String message) {
        if(level.getPriority() < config.getCurrentLevel().getPriority()) {
            return; //skipping lower level logs
        }
        
        LogMessage logMessage = new LogMessage(level, message);
        
        lock.lock();
        
        try {
            for (Appender appender : config.getAppenders()) {
                appender.append(logMessage);
            }
        } finally {
            lock.unlock();
        }
    }
    
    // Helper methods
    public void debug(String msg) { log(LogLevel.DEBUG, msg); }
    public void info(String msg) { log(LogLevel.INFO, msg); }
    public void warning(String msg) { log(LogLevel.WARNING, msg); }
    public void error(String msg) { log(LogLevel.ERROR, msg); }
    public void fatal(String msg) { log(LogLevel.FATAL, msg); }
}

public class LoggingFrameworkDemo {
    public static void main(String[] args) {
        
        //Configure Logger 
        // Configure logger
        LoggerConfig config = new LoggerConfig(LogLevel.DEBUG);
        config.addAppender(new ConsoleAppender());
        config.addAppender(new FileAppender("app.log"));
        config.addAppender(new DatabaseAppender());

        Logger logger = new Logger(config);

        // Logging
        logger.debug("Debugging details...");
        logger.info("System is running fine.");
        logger.warning("Low memory warning.");
        logger.error("File not found error!");
        logger.fatal("System crash!");

        // Change config dynamically
        config.setCurrentLevel(LogLevel.ERROR);
        logger.info("This won't be logged (INFO < ERROR).");
        logger.error("Only ERROR and above are logged now.");
    }
}
        