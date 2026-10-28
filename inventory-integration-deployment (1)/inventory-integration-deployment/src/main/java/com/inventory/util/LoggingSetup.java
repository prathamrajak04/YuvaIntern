package com.inventory.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Configures logging for the whole application using only the JDK (java.util.logging), so the
 * deployment needs no third-party dependency.
 *
 * <p>Behavior: INFO and above go to the log file; only WARNING and above go to the console (stderr),
 * so routine messages do not clutter the interactive menu. If the log file cannot be opened the
 * application still starts and logs to the console only.
 *
 * <p>Calling {@link #init} again replaces the previous handlers, so tests and restarts do not
 * accumulate duplicate handlers.
 */
public final class LoggingSetup {

    private static final String ROOT_PACKAGE = "com.inventory";

    private LoggingSetup() {
        // utility class
    }

    public static void init(Path logFile) {
        Logger logger = Logger.getLogger(ROOT_PACKAGE);
        for (Handler old : logger.getHandlers()) {
            logger.removeHandler(old);
            old.close();
        }
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.INFO);

        ConsoleHandler console = new ConsoleHandler();
        console.setLevel(Level.WARNING);
        console.setFormatter(new OneLineFormatter());
        logger.addHandler(console);

        try {
            Path absolute = logFile.toAbsolutePath();
            if (absolute.getParent() != null) {
                Files.createDirectories(absolute.getParent());
            }
            FileHandler file = new FileHandler(absolute.toString(), true); // append mode
            file.setLevel(Level.INFO);
            file.setFormatter(new OneLineFormatter());
            file.setEncoding("UTF-8");
            logger.addHandler(file);
        } catch (IOException | SecurityException e) {
            logger.log(Level.WARNING, "Could not open log file {0}: {1}", new Object[] {logFile, e.getMessage()});
        }
    }

    /** One readable line per record: {@code 2026-10-26 14:02:11 INFO  InventoryService - Added item A1}. */
    private static final class OneLineFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            String time = java.time.LocalDateTime.ofInstant(record.getInstant(), java.time.ZoneId.systemDefault())
                    .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                    .toString().replace('T', ' ');
            String loggerName = record.getLoggerName();
            String shortName = loggerName == null ? "-" : loggerName.substring(loggerName.lastIndexOf('.') + 1);
            return String.format("%s %-7s %s - %s%n", time, record.getLevel().getName(), shortName, formatMessage(record));
        }
    }
}
