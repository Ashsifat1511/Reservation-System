package com.oasis.reservation.db;

import com.oasis.reservation.AppLog;
import com.oasis.reservation.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class UserDao {

    private static final Logger LOG = AppLog.get(UserDao.class);
    /**
     * Verified against when the username does not exist, so a wrong username takes as long as a
     * wrong password and response time does not reveal which accounts exist.
     */
    private static final String DUMMY_HASH = PasswordHasher.hash("timing-equaliser".toCharArray());

    private final Database db;

    public UserDao(Database db) {
        this.db = db;
    }

    /** Returns true only if the user exists and the password matches the stored hash. */
    public boolean authenticate(String username, char[] password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.length == 0) {
            return false;
        }
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE username = ?")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                String stored = rs.next() ? rs.getString(1) : null;
                boolean ok = PasswordHasher.verify(password, stored != null ? stored : DUMMY_HASH) && stored != null;
                // Never log the password. Username is logged to help diagnose lockouts.
                LOG.info((ok ? "Login succeeded" : "Login failed") + " for user '" + sanitize(username) + "'");
                return ok;
            }
        }
    }

    /** Strips control characters and caps length so user input cannot forge log lines. */
    static String sanitize(String s) {
        String clean = s.trim().replaceAll("\\p{Cntrl}", "?");
        return clean.length() > 50 ? clean.substring(0, 50) + "..." : clean;
    }
}
