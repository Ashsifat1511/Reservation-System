package com.oasis.reservation.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class TrainDao {

    private final Database db;

    public TrainDao(Database db) {
        this.db = db;
    }

    public Optional<String> findTrainName(int trainNumber) throws SQLException {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT train_name FROM trains WHERE train_number = ?")) {
            ps.setInt(1, trainNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        }
    }
}
