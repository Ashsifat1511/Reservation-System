package com.oasis.reservation.model;

import java.time.LocalDate;

/** A single booking row. Immutable. */
public record Reservation(
        String pnr,
        String bookedBy,
        String passengerName,
        int trainNumber,
        String trainName,
        String classType,
        LocalDate journeyDate,
        String source,
        String destination) {

    /** Multi-line summary used by the confirmation dialog and the cancellation form. */
    public String toDisplayText() {
        return String.join(System.lineSeparator(),
                "PNR Number      : " + pnr,
                "Passenger Name  : " + passengerName,
                "Train Number    : " + trainNumber,
                "Train Name      : " + trainName,
                "Class Type      : " + classType,
                "Date of Journey : " + journeyDate,
                "From            : " + source,
                "To              : " + destination,
                "Booked By       : " + bookedBy);
    }
}
