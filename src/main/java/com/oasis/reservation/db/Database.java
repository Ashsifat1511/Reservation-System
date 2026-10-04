package com.oasis.reservation.db;

import com.oasis.reservation.security.PasswordHasher;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the SQLite connection URL, creates the schema and seeds reference data on first run.
 * The database file defaults to {@code reservation.db} in the working directory and can be
 * moved with {@code -Dreservation.db=path/to/file.db}.
 */
public final class Database {

    public static final String DEFAULT_DB_FILE = "reservation.db";

    /** Demo account created on first run. Password can be overridden; see README. */
    public static final String DEMO_USERNAME = "demo";
    private static final String DEMO_PASSWORD_ENV = "RESERVATION_DEMO_PASSWORD";
    private static final String DEMO_PASSWORD_DEFAULT = "demo@123";

    /** Sample trains used to auto-populate the train name from the train number. */
    private static final Object[][] SEED_TRAINS = {
            {12301, "Rajdhani Express"},
            {12002, "Shatabdi Express"},
            {12951, "Mumbai Rajdhani"},
            {12627, "Karnataka Express"},
            {12839, "Howrah Mail"},
            {22691, "Bengaluru Rajdhani"},
            {12259, "Duronto Express"},
            {12049, "Gatimaan Express"},
    };

    /** Bump when the schema changes; stored in SQLite's {@code PRAGMA user_version}. */
    public static final int SCHEMA_VERSION = 1;
    /** How long a connection waits for another process's lock before failing (ms). */
    static final int BUSY_TIMEOUT_MS = 5_000;

    /** Column limits, enforced both by BookingValidator and by CHECK constraints. */
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_STATION_LENGTH = 60;
    public static final int MAX_CLASS_LENGTH = 40;

    private final String url;
    private final Path file;

    public Database(String dbFile) {
        this.file = Path.of(dbFile).toAbsolutePath();
        this.url = "jdbc:sqlite:" + dbFile;
    }

    public Path location() {
        return file;
    }

    public static Database fromSystemProperty() {
        return new Database(System.getProperty("reservation.db", DEFAULT_DB_FILE));
    }

    public Connection connect() throws SQLException {
        Connection c = DriverManager.getConnection(url);
        try (Statement s = c.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS); // constant, not user input
        }
        return c;
    }

    /** Creates tables if missing and inserts the demo user and sample trains once. */
    public void initialise() throws SQLException {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            int version = schemaVersion(c);
            if (version > SCHEMA_VERSION) {
                throw new SQLException("Database schema version " + version
                        + " is newer than this app supports (" + SCHEMA_VERSION + "). Use a newer build.");
            }
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (
                        id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        username      TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL
                    )""");
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS trains (
                        train_number INTEGER PRIMARY KEY,
                        train_name   TEXT NOT NULL
                    )""");
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        pnr            TEXT PRIMARY KEY,
                        booked_by      TEXT NOT NULL REFERENCES users(username),
                        passenger_name TEXT NOT NULL CHECK (length(passenger_name) BETWEEN 1 AND %d),
                        train_number   INTEGER NOT NULL REFERENCES trains(train_number),
                        train_name     TEXT NOT NULL,
                        class_type     TEXT NOT NULL CHECK (length(class_type) BETWEEN 1 AND %d),
                        journey_date   TEXT NOT NULL,
                        source         TEXT NOT NULL CHECK (length(source) BETWEEN 1 AND %d),
                        destination    TEXT NOT NULL CHECK (length(destination) BETWEEN 1 AND %d),
                        created_at     TEXT NOT NULL DEFAULT (datetime('now'))
                    )""".formatted(MAX_NAME_LENGTH, MAX_CLASS_LENGTH, MAX_STATION_LENGTH, MAX_STATION_LENGTH));
            seedTrains(c);
            seedDemoUser(c);
            if (version < SCHEMA_VERSION) {
                s.execute("PRAGMA user_version = " + SCHEMA_VERSION); // constant, not user input
            }
        }
    }

    public int schemaVersion() throws SQLException {
        try (Connection c = connect()) {
            return schemaVersion(c);
        }
    }

    private static int schemaVersion(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void seedTrains(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT OR IGNORE INTO trains(train_number, train_name) VALUES (?, ?)")) {
            for (Object[] t : SEED_TRAINS) {
                ps.setInt(1, (Integer) t[0]);
                ps.setString(2, (String) t[1]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedDemoUser(Connection c) throws SQLException {
        try (PreparedStatement check = c.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            check.setString(1, DEMO_USERNAME);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        String fromEnv = System.getenv(DEMO_PASSWORD_ENV);
        char[] password = (fromEnv != null && !fromEnv.isBlank() ? fromEnv : DEMO_PASSWORD_DEFAULT).toCharArray();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO users(username, password_hash) VALUES (?, ?)")) {
            ps.setString(1, DEMO_USERNAME);
            ps.setString(2, PasswordHasher.hash(password));
            ps.executeUpdate();
        } finally {
            PasswordHasher.wipe(password);
        }
    }
}
