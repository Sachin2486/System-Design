import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

// Enum for log levels
enum LogLevel {
    DEBUG(1), INFO(2), WARNING(3), ERROR(4), FATAL(5);

    private final int severity;

    LogLevel(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return severity;
    }
}

// Represents a log message with timestamp, level, and content
class LogMessage {
    private final Date timestamp;
    private final LogLevel level;
    private final String message;

    public LogMessage(LogLevel level, String message) {
        this.timestamp = new Date();
        this.level = level;
        this.message = message;
    }

    public String format() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return String.format("[%s] [%s] %s", sdf.format(timestamp), level, message);
    }

    public LogLevel getLevel() {
        return level;
    }
}

// Interface for log destinations (extensible)
interface LogDestination {
    void write(LogMessage message);
}

// Console logging
class ConsoleDestination implements LogDestination {
    @Override
    public void write(LogMessage message) {
        System.out.println(message.format());
    }
}

// File logging
class FileDestination implements LogDestination {
    private final String filePath;

    public FileDestination(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public synchronized void write(LogMessage message) {
        try (FileWriter fw = new FileWriter(filePath, true)) {
            fw.write(message.format() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Database logging (for demo, using SQLite)
class DatabaseDestination implements LogDestination {
    private final Connection connection;

    public DatabaseDestination(String dbUrl) throws SQLException {
        connection = DriverManager.getConnection(dbUrl);
        initTable();
    }

    private void initTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY AUTOINCREMENT, timestamp TEXT, level TEXT, message TEXT)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    @Override
    public synchronized void write(LogMessage message) {
        String sql = "INSERT INTO logs (timestamp, level, message) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            pstmt.setString(2, message.getLevel().toString());
            pstmt.setString(3, message.format());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

// LoggerConfig: manages log level and destinations
class LoggerConfig {
    private LogLevel minLevel;
    private final List<LogDestination> destinations;

    public LoggerConfig(LogLevel minLevel) {
        this.minLevel = minLevel;
        this.destinations = new ArrayList<>();
    }

    public synchronized void addDestination(LogDestination destination) {
        destinations.add(destination);
    }

    public synchronized List<LogDestination> getDestinations() {
        return new ArrayList<>(destinations);
    }

    public LogLevel getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(LogLevel minLevel) {
        this.minLevel = minLevel;
    }
}

// Logger: thread-safe logging manager
class Logger {
    private final LoggerConfig config;
    private final ReentrantLock lock = new ReentrantLock();

    public Logger(LoggerConfig config) {
        this.config = config;
    }

    public void log(LogLevel level, String message) {
        if (level.getSeverity() >= config.getMinLevel().getSeverity()) {
            LogMessage logMessage = new LogMessage(level, message);
            lock.lock();
            try {
                for (LogDestination dest : config.getDestinations()) {
                    dest.write(logMessage);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public void debug(String message) { log(LogLevel.DEBUG, message); }
    public void info(String message) { log(LogLevel.INFO, message); }
    public void warn(String message) { log(LogLevel.WARNING, message); }
    public void error(String message) { log(LogLevel.ERROR, message); }
    public void fatal(String message) { log(LogLevel.FATAL, message); }
}

// Demo class
public class LoggingDemo {
    public static void main(String[] args) throws Exception {
        // Create config with minimum log level = DEBUG
        LoggerConfig config = new LoggerConfig(LogLevel.DEBUG);

        // Add destinations
        config.addDestination(new ConsoleDestination());
        config.addDestination(new FileDestination("logs.txt"));

        // (Optional) Database logging
        // config.addDestination(new DatabaseDestination("jdbc:sqlite:logs.db"));

        // Create logger
        Logger logger = new Logger(config);

        // Log messages
        logger.debug("This is a DEBUG message");
        logger.info("Application started successfully");
        logger.warn("Low disk space warning");
        logger.error("Error connecting to service");
        logger.fatal("System crash!");

        System.out.println("Logs written to console and file.");
    }
}
