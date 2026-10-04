package com.oasis.reservation;

import com.oasis.reservation.service.BookingValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookingValidatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

    private List<String> validate(String name, String trainNo, String trainName, String cls,
                                  String date, String src, String dst) {
        return BookingValidator.validate(name, trainNo, trainName, cls, date, src, dst, TODAY);
    }

    @Test
    void validInputHasNoErrors() {
        assertTrue(validate("Sifat", "12301", "Rajdhani Express", "Sleeper (SL)",
                "2026-10-10", "Dhaka", "Khulna").isEmpty());
    }

    @Test
    void todayIsAllowed() {
        assertTrue(validate("A", "12301", "X", "SL", "2026-10-04", "A", "B").isEmpty());
    }

    @Test
    void emptyFieldsAreReported() {
        List<String> errors = validate("", "", "", "", "", "", "");
        assertTrue(errors.contains("Passenger name is required."));
        assertTrue(errors.contains("Train number is required."));
        assertTrue(errors.contains("Class type is required."));
        assertTrue(errors.contains("Date of journey is required."));
        assertTrue(errors.contains("Source station is required."));
        assertTrue(errors.contains("Destination station is required."));
    }

    @Test
    void nonNumericTrainNumberRejected() {
        assertTrue(validate("A", "12a01", "", "SL", "2026-10-10", "A", "B")
                .contains("Train number must be numeric."));
    }

    @Test
    void unknownTrainNumberRejected() {
        assertTrue(validate("A", "99999", "", "SL", "2026-10-10", "A", "B")
                .contains("Train number 99999 was not found."));
    }

    @Test
    void badDateFormatsRejected() {
        for (String bad : new String[]{"10/10/2026", "2026-13-01", "2026-02-30", "2026-1-5", "tomorrow"}) {
            assertNull(BookingValidator.parseDate(bad), bad);
        }
        assertEquals(LocalDate.of(2028, 2, 29), BookingValidator.parseDate("2028-02-29"));
    }

    @Test
    void pastDateRejected() {
        assertTrue(validate("A", "12301", "X", "SL", "2026-10-03", "A", "B")
                .contains("Date of journey cannot be in the past."));
    }

    @Test
    void sameSourceAndDestinationRejected() {
        assertTrue(validate("A", "12301", "X", "SL", "2026-10-10", "Dhaka", " dhaka ")
                .contains("Source and destination stations must be different."));
    }
}
