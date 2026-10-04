package com.oasis.reservation;

import com.oasis.reservation.db.Database;
import com.oasis.reservation.model.Reservation;
import com.oasis.reservation.service.PnrGenerator;
import com.oasis.reservation.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ReservationDbTest {

    @TempDir
    Path tmp;
    private AppContext ctx;

    @BeforeEach
    void setUp() throws Exception {
        Database db = new Database(tmp.resolve("test.db").toString());
        db.initialise();
        db.initialise(); // second run must not duplicate seed data or fail
        ctx = AppContext.create(db);
    }

    private Reservation bookSample() throws Exception {
        return ctx.reservationService().book("demo", "Sifat", "12301", "Sleeper (SL)",
                LocalDate.now().plusDays(30).toString(), "Dhaka", "Khulna");
    }

    @Test
    void demoUserCanLogInAndWrongPasswordIsDenied() throws Exception {
        assertTrue(ctx.users().authenticate(Database.DEMO_USERNAME, "demo@123".toCharArray()));
        assertFalse(ctx.users().authenticate(Database.DEMO_USERNAME, "wrong".toCharArray()));
        assertFalse(ctx.users().authenticate("nobody", "demo@123".toCharArray()));
    }

    @Test
    void sqlInjectionInUsernameDoesNotBypassLogin() throws Exception {
        assertFalse(ctx.users().authenticate("' OR '1'='1", "' OR '1'='1".toCharArray()));
    }

    @Test
    void trainNameIsLookedUpFromNumber() throws Exception {
        assertEquals(Optional.of("Rajdhani Express"), ctx.trains().findTrainName(12301));
        assertEquals(Optional.empty(), ctx.trains().findTrainName(1));
    }

    @Test
    void bookFetchCancelRoundTrip() throws Exception {
        Reservation booked = bookSample();
        assertTrue(PnrGenerator.isWellFormed(booked.pnr()));

        Reservation fetched = ctx.reservations().findByPnr(booked.pnr()).orElseThrow();
        assertEquals(booked, fetched);

        assertTrue(ctx.reservations().deleteByPnr(booked.pnr()));
        assertTrue(ctx.reservations().findByPnr(booked.pnr()).isEmpty());
        assertFalse(ctx.reservations().deleteByPnr(booked.pnr()));
    }

    @Test
    void generatedPnrsAreUnique() throws Exception {
        Set<String> pnrs = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            assertTrue(pnrs.add(bookSample().pnr()));
        }
    }

    @Test
    void collidingPnrIsRetried() throws Exception {
        Reservation first = bookSample();
        Deque<String> scripted = new ArrayDeque<>(List.of(first.pnr(), first.pnr(), "1234567890"));
        PnrGenerator stub = new PnrGenerator() {
            @Override
            public String next() {
                return scripted.pop();
            }
        };
        ReservationService service = new ReservationService(ctx.reservations(), ctx.trains(), stub,
                Clock.systemDefaultZone());
        Reservation second = service.book("demo", "B", "12301", "SL",
                LocalDate.now().plusDays(31).toString(), "A", "B");
        assertEquals("1234567890", second.pnr());
    }

    @Test
    void pnrFormat() {
        assertTrue(PnrGenerator.isWellFormed("1000000000"));
        assertFalse(PnrGenerator.isWellFormed("0123456789"));
        assertFalse(PnrGenerator.isWellFormed("12345"));
        assertFalse(PnrGenerator.isWellFormed("12345abcde"));
        PnrGenerator g = new PnrGenerator();
        for (int i = 0; i < 1000; i++) {
            assertTrue(PnrGenerator.isWellFormed(g.next()));
        }
    }
}
