package com.oasis.reservation;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * File-based error log using java.util.logging (no extra dependency).
 * Default file: {@code reservation.log} in the working directory, rotated at 1 MB, 3 files kept.
 * Override with {@code -Dreservation.log=path/to/file.log}.
 * Technical details (SQL errors, stack traces) go here; users only see a friendly message.
 */
public final class AppLog {

    public static final String DEFAULT_LOG_FILE = "reservation.log";
    private static final int MAX_BYTES = 1_000_000;
    private static final int FILE_COUNT = 3;
    private static final Logger ROOT = Logger.getLogger("com.oasis.reservation");

    private AppLog() {
    }

    public static void init() {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$-7s %3$s - %5$s%6$s%n");
        String file = System.getProperty("reservation.log", DEFAULT_LOG_FILE);
        try {
            FileHandler handler = new FileHandler(file.replace("%", "%%"), MAX_BYTES, FILE_COUNT, true);
            handler.setFormatter(new SimpleFormatter());
            handler.setLevel(Level.INFO);
            ROOT.addHandler(handler);
            ROOT.setLevel(Level.INFO);
        } catch (IOException | SecurityException e) {
            // Logging must never stop the app from starting; fall back to the console.
            System.err.println("Could not open log file " + file + ": " + e.getMessage());
        }
    }

    public static Logger get(Class<?> type) {
        return Logger.getLogger(type.getName());
    }
}
