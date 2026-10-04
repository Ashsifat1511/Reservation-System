package com.oasis.reservation;

import com.oasis.reservation.db.Database;
import com.oasis.reservation.db.ReservationDao;
import com.oasis.reservation.db.TrainDao;
import com.oasis.reservation.db.UserDao;
import com.oasis.reservation.service.PnrGenerator;
import com.oasis.reservation.service.ReservationService;

import java.time.Clock;

/** Wires the data-access objects and services together once at start-up. */
public record AppContext(UserDao users, TrainDao trains, ReservationDao reservations,
                         ReservationService reservationService) {

    public static AppContext create(Database db) {
        ReservationDao reservations = new ReservationDao(db);
        TrainDao trains = new TrainDao(db);
        return new AppContext(new UserDao(db), trains, reservations,
                new ReservationService(reservations, trains, new PnrGenerator(), Clock.systemDefaultZone()));
    }
}
