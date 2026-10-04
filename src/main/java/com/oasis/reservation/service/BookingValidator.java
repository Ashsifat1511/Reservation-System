package com.oasis.reservation.service;

import com.oasis.reservation.db.Database;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/** Pure input validation for the reservation form. Returns human-readable error messages. */
public final class BookingValidator {

    public static final String DATE_PATTERN = "yyyy-MM-dd";
    /** {@code uuuu} + STRICT rejects impossible dates such as 2026-02-30. */
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private BookingValidator() {
    }

    public static List<String> validate(String passengerName, String trainNumber, String trainName,
                                        String classType, String journeyDate, String source,
                                        String destination, LocalDate today) {
        List<String> errors = new ArrayList<>();
        requireText(errors, passengerName, "Passenger name", Database.MAX_NAME_LENGTH);
        requireText(errors, classType, "Class type", Database.MAX_CLASS_LENGTH);
        requireText(errors, source, "Source station", Database.MAX_STATION_LENGTH);
        requireText(errors, destination, "Destination station", Database.MAX_STATION_LENGTH);

        if (isBlank(trainNumber)) {
            errors.add("Train number is required.");
        } else if (!isNumeric(trainNumber)) {
            errors.add("Train number must be numeric.");
        } else if (isBlank(trainName)) {
            errors.add("Train number " + trainNumber.trim() + " was not found.");
        }

        if (isBlank(journeyDate)) {
            errors.add("Date of journey is required.");
        } else {
            LocalDate date = parseDate(journeyDate);
            if (date == null) {
                errors.add("Date of journey must be a valid date in " + DATE_PATTERN + " format.");
            } else if (date.isBefore(today)) {
                errors.add("Date of journey cannot be in the past.");
            }
        }

        if (!isBlank(source) && !isBlank(destination)
                && source.trim().equalsIgnoreCase(destination.trim())) {
            errors.add("Source and destination stations must be different.");
        }
        return errors;
    }

    /** Digits only, 1–9 characters so it always fits in an int. */
    public static boolean isNumeric(String s) {
        return s != null && s.trim().matches("\\d{1,9}");
    }

    /** @return the parsed date, or null if the text is not a real yyyy-MM-dd date */
    public static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s.trim(), DATE_FORMAT);
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }

    private static void requireText(List<String> errors, String value, String label, int maxLength) {
        if (isBlank(value)) {
            errors.add(label + " is required.");
        } else if (value.trim().length() > maxLength) {
            errors.add(label + " must be at most " + maxLength + " characters.");
        } else if (value.chars().anyMatch(Character::isISOControl)) {
            errors.add(label + " contains invalid characters.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
