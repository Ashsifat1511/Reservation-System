package com.oasis.reservation;

import com.oasis.reservation.db.Database;
import com.oasis.reservation.model.Reservation;
import com.oasis.reservation.security.PasswordHasher;
import com.oasis.reservation.service.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Checks added by the pre-release audit (security, invalid input, failure modes). */
class SecurityAndEdgeCaseTest {

    @TempDir
    Path tmp;
    private Database db;
    private AppContext ctx;
    private final String future = LocalDate.now().plusDays(5).toString();

    @BeforeEach
    void setUp() throws Exception {
        db = new Database(tmp.resolve("t.db").toString());
        db.initialise();
        ctx = AppContext.create(db);
    }

    // ---- service-layer validation (the DB never trusts the form) ----

    @Test
    void serviceRejectsInvalidInputEvenIfUiIsBypassed() {
        ValidationException ex = assertThrows(ValidationException.class, () ->
                ctx.reservationService().book("demo", "", "abc", "", "31/12/2026", "", ""));
        assertTrue(ex.errors().size() >= 5, ex.errors().toString());
    }

    @Test
    void serviceLooksUpTrainNameItselfAndRejectsUnknownTrains() throws Exception {
        assertThrows(ValidationException.class, () ->
                ctx.reservationService().book("demo", "A", "99999", "SL", future, "X", "Y"));
        Reservation r = ctx.reservationService().book("demo", "A", " 12301 ", "SL", future, "X", "Y");
        assertEquals("Rajdhani Express", r.trainName());
    }

    @Test
    void overlongAndControlCharacterInputIsRejected() {
        String longName = "x".repeat(Database.MAX_NAME_LENGTH + 1);
        ValidationException ex = assertThrows(ValidationException.class, () ->
                ctx.reservationService().book("demo", longName, "12301", "SL", future, "A\u0000", "B"));
        assertTrue(ex.errors().contains("Passenger name must be at most 100 characters."));
        assertTrue(ex.errors().contains("Source station contains invalid characters."));
    }

    @Test
    void unicodeNamesAreAccepted() throws Exception {
        Reservation r = ctx.reservationService().book("demo", "সিফাত O'Brien-Ünal", "12301", "SL",
                future, "খুলনা", "ঢাকা");
        assertEquals("সিফাত O'Brien-Ünal", ctx.reservations().findByPnr(r.pnr()).orElseThrow().passengerName());
    }

    @Test
    void sqlMetacharactersAreStoredLiterallyNotExecuted() throws Exception {
        String evil = "x'); DROP TABLE reservations; --";
        Reservation r = ctx.reservationService().book("demo", evil, "12301", "SL", future, "A", "B");
        assertEquals(evil, ctx.reservations().findByPnr(r.pnr()).orElseThrow().passengerName());
        assertTrue(ctx.reservations().pnrExists(r.pnr())); // table still exists
    }

    @Test
    void lookupOfInjectionStylePnrFindsNothing() throws Exception {
        ctx.reservationService().book("demo", "A", "12301", "SL", future, "X", "Y");
        assertTrue(ctx.reservations().findByPnr("' OR '1'='1").isEmpty());
        assertFalse(ctx.reservations().deleteByPnr("' OR '1'='1"));
    }

    @Test
    void databaseCheckConstraintBacksUpTheValidator() throws Exception {
        try (Connection c = db.connect(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reservations(pnr, booked_by, passenger_name, train_number, train_name, class_type,"
                        + " journey_date, source, destination) VALUES ('1111111111','demo',?,12301,'R','SL','2030-01-01','A','B')")) {
            ps.setString(1, "y".repeat(Database.MAX_NAME_LENGTH + 1));
            assertThrows(SQLException.class, ps::executeUpdate);
        }
    }

    @Test
    void foreignKeysRejectUnknownTrainOrUser() throws Exception {
        try (Connection c = db.connect(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.executeUpdate(
                    "INSERT INTO reservations(pnr, booked_by, passenger_name, train_number, train_name, class_type,"
                            + " journey_date, source, destination) VALUES ('2222222222','ghost','A',12301,'R','SL','2030-01-01','A','B')"));
        }
    }

    // ---- authentication ----

    @Test
    void passwordIsNeverStoredInPlaintext() throws Exception {
        try (Connection c = db.connect(); Statement s = c.createStatement();
             var rs = s.executeQuery("SELECT password_hash FROM users")) {
            assertTrue(rs.next());
            assertFalse(rs.getString(1).contains("demo@123"));
            assertTrue(rs.getString(1).startsWith(PasswordHasher.ITERATIONS + ":"));
        }
    }

    @Test
    void unknownUserIsRejectedAndTakesComparableTime() throws Exception {
        long t0 = System.nanoTime();
        assertFalse(ctx.users().authenticate("demo", "wrong".toCharArray()));
        long knownUser = System.nanoTime() - t0;
        t0 = System.nanoTime();
        assertFalse(ctx.users().authenticate("no-such-user", "wrong".toCharArray()));
        long unknownUser = System.nanoTime() - t0;
        // Without the dummy hash an unknown user returns ~100x faster. Allow generous jitter.
        assertTrue(unknownUser > knownUser / 4, "unknown=" + unknownUser + " known=" + knownUser);
    }

    @Test
    void blankAndNullCredentialsAreRejected() throws Exception {
        assertFalse(ctx.users().authenticate("", "x".toCharArray()));
        assertFalse(ctx.users().authenticate("demo", new char[0]));
        assertFalse(ctx.users().authenticate(null, null));
    }

    @Test
    void tamperedIterationCountIsRejectedQuickly() {
        long t0 = System.nanoTime();
        assertFalse(PasswordHasher.verify("x".toCharArray(), "2000000000:AAAA:AAAA"));
        assertFalse(PasswordHasher.verify("x".toCharArray(), "1:AAAA:AAAA"));
        assertTrue(System.nanoTime() - t0 < 1_000_000_000L);
    }

    // ---- database lifecycle / failure modes ----

    @Test
    void schemaVersionIsRecorded() throws Exception {
        assertEquals(Database.SCHEMA_VERSION, db.schemaVersion());
    }

    @Test
    void newerSchemaIsRefusedInsteadOfCorrupted() throws Exception {
        try (Connection c = db.connect(); Statement s = c.createStatement()) {
            s.execute("PRAGMA user_version = " + (Database.SCHEMA_VERSION + 1));
        }
        SQLException ex = assertThrows(SQLException.class, db::initialise);
        assertTrue(ex.getMessage().contains("newer"));
    }

    @Test
    void corruptDatabaseFileFailsCleanly() throws Exception {
        Path bad = tmp.resolve("corrupt.db");
        Files.writeString(bad, "this is not a sqlite database ".repeat(200));
        assertThrows(SQLException.class, () -> new Database(bad.toString()).initialise());
    }

    @Test
    void missingFolderFailsCleanly() {
        Path nowhere = tmp.resolve("no/such/folder/x.db");
        assertThrows(SQLException.class, () -> new Database(nowhere.toString()).initialise());
    }

    @Test
    void concurrentBookingsFromTwoAppInstancesAllSucceed() throws Exception {
        // Two independent contexts on the same file simulate two open app windows.
        AppContext other = AppContext.create(new Database(db.location().toString()));
        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Reservation>> results = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            AppContext c = (i % 2 == 0) ? ctx : other;
            results.add(pool.submit(() -> c.reservationService().book("demo", "P", "12301", "SL", future, "A", "B")));
        }
        for (Future<Reservation> f : results) {
            assertNotNull(f.get().pnr());
        }
        pool.shutdown();
        try (Connection c = db.connect(); Statement s = c.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM reservations")) {
            rs.next();
            assertEquals(40, rs.getInt(1));
        }
    }
}
