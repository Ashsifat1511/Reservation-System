package com.oasis.reservation.service;

import com.oasis.reservation.db.ReservationDao;
import com.oasis.reservation.db.TrainDao;
import com.oasis.reservation.model.Reservation;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Books reservations under a freshly generated, unique PNR.
 * All input is re-validated here and the train name is looked up from the database, so the
 * stored data never depends on what the UI claims (the UI checks are only for fast feedback).
 */
public class ReservationService {

    private static final int MAX_PNR_ATTEMPTS = 20;

    private final ReservationDao dao;
    private final TrainDao trains;
    private final PnrGenerator pnrGenerator;
    private final Clock clock;

    public ReservationService(ReservationDao dao, TrainDao trains, PnrGenerator pnrGenerator, Clock clock) {
        this.dao = dao;
        this.trains = trains;
        this.pnrGenerator = pnrGenerator;
        this.clock = clock;
    }

    public Reservation book(String bookedBy, String passengerName, String trainNumberText, String classType,
                            String journeyDateText, String source, String destination)
            throws SQLException, ValidationException {
        String trainName = "";
        if (BookingValidator.isNumeric(trainNumberText)) {
            trainName = trains.findTrainName(Integer.parseInt(trainNumberText.trim())).orElse("");
        }
        List<String> errors = BookingValidator.validate(passengerName, trainNumberText, trainName, classType,
                journeyDateText, source, destination, LocalDate.now(clock));
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        int trainNumber = Integer.parseInt(trainNumberText.trim());
        LocalDate journeyDate = BookingValidator.parseDate(journeyDateText);

        for (int attempt = 0; attempt < MAX_PNR_ATTEMPTS; attempt++) {
            String pnr = pnrGenerator.next();
            if (dao.pnrExists(pnr)) {
                continue;
            }
            Reservation r = new Reservation(pnr, bookedBy, passengerName.trim(), trainNumber, trainName,
                    classType.trim(), journeyDate, source.trim(), destination.trim());
            dao.insert(r); // PRIMARY KEY on pnr is the final uniqueness guard
            return r;
        }
        throw new SQLException("Could not generate a unique PNR after " + MAX_PNR_ATTEMPTS + " attempts");
    }
}
