import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class LoggingFrameworkDemo {
    enum LogLevel {

        DEBUG(1),
        INFO(2),
        WARNING(3),
        ERROR(4),
        FATAL(5);

        private final int severity;

        LogLevel(int severity) {
            this.severity = severity;
        }

        public int getSeverity() {
            return severity;
        }
    }

    static class LogMessage {

        private final LocalDateTime timestamp;
        private final LogLevel level;
        private final String message;
        private final String threadName;

        public LogMessage(LogLevel level,
                          String message) {

            this.timestamp = LocalDateTime.now();
            this.level = level;
            this.message = message;
            this.threadName = Thread.currentThread().getName();
        }

        @Override
        public String toString() {

            return String.format(
                    "[%s] [%s] [%s] %s",
                    timestamp,
                    level,
                    threadName,
                    message
            );
        }
    }


    interface LogAppender {

        void append(LogMessage message);

    }

    static class ConsoleAppender
            implements LogAppender {

        private final ReentrantLock lock =
                new ReentrantLock();

        @Override
        public void append(LogMessage message) {

            lock.lock();

            try {

                System.out.println(message);

            }
            finally {

                lock.unlock();

            }
        }
    }

    static class FileAppender
            implements LogAppender {

        private final String fileName;

        private final ReentrantLock lock =
                new ReentrantLock();

        public FileAppender(String fileName) {

            this.fileName = fileName;

        }

        @Override
        public void append(LogMessage message) {

            lock.lock();

            try (BufferedWriter writer =
                         new BufferedWriter(
                                 new FileWriter(fileName,
                                         true))) {

                writer.write(message.toString());

                writer.newLine();

            }
            catch (IOException e) {

                e.printStackTrace();

            }
            finally {

                lock.unlock();

            }

        }

    }


    static class DatabaseAppender
            implements LogAppender {

        private final ReentrantLock lock =
                new ReentrantLock();

        @Override
        public void append(LogMessage message) {

            lock.lock();

            try {

                // Imagine JDBC operation here

                System.out.println(
                        "Persisted To DB : "
                                + message);

            }
            finally {

                lock.unlock();

            }

        }

    }

    static class LoggerConfig {

        private final LogLevel minimumLevel;

        private final List<LogAppender>
                appenders;

        public LoggerConfig(
                LogLevel minimumLevel,

                List<LogAppender> appenders) {

            this.minimumLevel =
                    minimumLevel;

            this.appenders =
                    appenders;

        }

        public LogLevel getMinimumLevel() {

            return minimumLevel;

        }

        public List<LogAppender>
        getAppenders() {

            return appenders;

        }

    }

    static class Logger {

        private static volatile Logger instance;

        private LoggerConfig config;

        private Logger(
                LoggerConfig config) {

            this.config = config;

        }

        public static Logger getInstance(
                LoggerConfig config) {

            if (instance == null) {

                synchronized (
                        Logger.class) {

                    if (instance == null) {

                        instance =
                                new Logger(
                                        config);

                    }

                }

            }

            return instance;

        }

        public void log(
                LogLevel level,

                String message) {

            if (level.getSeverity()

                    <

                    config.getMinimumLevel()
                            .getSeverity()) {

                return;

            }

            LogMessage logMessage =

                    new LogMessage(
                            level,
                            message);

            for (LogAppender appender :

                    config.getAppenders()) {

                appender.append(
                        logMessage);

            }

        }

        public void debug(
                String msg) {

            log(
                    LogLevel.DEBUG,
                    msg);

        }

        public void info(
                String msg) {

            log(
                    LogLevel.INFO,
                    msg);

        }

        public void warning(
                String msg) {

            log(
                    LogLevel.WARNING,
                    msg);

        }

        public void error(
                String msg) {

            log(
                    LogLevel.ERROR,
                    msg);

        }

        public void fatal(
                String msg) {

            log(
                    LogLevel.FATAL,
                    msg);

        }

    }

    public static void main(
            String[] args) {

        LogAppender console =

                new ConsoleAppender();

        LogAppender file =

                new FileAppender(
                        "application.log");

        LogAppender db =

                new DatabaseAppender();


        LoggerConfig config =

                new LoggerConfig(

                        LogLevel.INFO,

                        List.of(

                                console,

                                file,

                                db

                        )

                );


        Logger logger =

                Logger.getInstance(
                        config);


        logger.debug(
                "Debug Message");

        logger.info(
                "Application Started");


        logger.warning(
                "Memory Usage High");


        logger.error(
                "Connection Timeout");


        logger.fatal(
                "Application Crashed");


        Runnable task = () -> {

            for (int i = 1;
                 i <= 5;
                 i++) {

                logger.info(

                        Thread.currentThread()
                                .getName()

                                +

                                " message "

                                +

                                i

                );

            }

        };


        Thread t1 =
                new Thread(
                        task,
                        "Thread-1");

        Thread t2 =
                new Thread(
                        task,
                        "Thread-2");


        t1.start();

        t2.start();

    }

}