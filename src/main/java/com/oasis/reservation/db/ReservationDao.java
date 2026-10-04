package com.oasis.reservation.db;

import com.oasis.reservation.model.Reservation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationDao {

    private final Database db;

    public ReservationDao(Database db) {
        this.db = db;
    }

    public boolean pnrExists(String pnr) throws SQLException {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM reservations WHERE pnr = ?")) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Reservation r) throws SQLException {
        String sql = """
                INSERT INTO reservations
                    (pnr, booked_by, passenger_name, train_number, train_name,
                     class_type, journey_date, source, destination)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.pnr());
            ps.setString(2, r.bookedBy());
            ps.setString(3, r.passengerName());
            ps.setInt(4, r.trainNumber());
            ps.setString(5, r.trainName());
            ps.setString(6, r.classType());
            ps.setString(7, r.journeyDate().toString());
            ps.setString(8, r.source());
            ps.setString(9, r.destination());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> findByPnr(String pnr) throws SQLException {
        String sql = """
                SELECT pnr, booked_by, passenger_name, train_number, train_name,
                       class_type, journey_date, source, destination
                FROM reservations WHERE pnr = ?""";
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pnr);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Reservation(
                        rs.getString("pnr"),
                        rs.getString("booked_by"),
                        rs.getString("passenger_name"),
                        rs.getInt("train_number"),
                        rs.getString("train_name"),
                        rs.getString("class_type"),
                        LocalDate.parse(rs.getString("journey_date")),
                        rs.getString("source"),
                        rs.getString("destination")));
            }
        }
    }

    /** @return true if a row was deleted */
    public boolean deleteByPnr(String pnr) throws SQLException {
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("DELETE FROM reservations WHERE pnr = ?")) {
            ps.setString(1, pnr);
            return ps.executeUpdate() == 1;
        }
    }
}
