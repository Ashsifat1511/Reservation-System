package com.oasis.reservation;

import com.oasis.reservation.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    @Test
    void correctPasswordVerifies() {
        String stored = PasswordHasher.hash("s3cret!".toCharArray());
        assertTrue(PasswordHasher.verify("s3cret!".toCharArray(), stored));
    }

    @Test
    void wrongPasswordFails() {
        String stored = PasswordHasher.hash("s3cret!".toCharArray());
        assertFalse(PasswordHasher.verify("S3cret!".toCharArray(), stored));
    }

    @Test
    void sameInputGivesDifferentHashesBecauseOfSalt() {
        assertNotEquals(PasswordHasher.hash("abc".toCharArray()), PasswordHasher.hash("abc".toCharArray()));
    }

    @Test
    void hashDoesNotContainPlaintext() {
        assertFalse(PasswordHasher.hash("plaintext".toCharArray()).contains("plaintext"));
    }

    @Test
    void malformedStoredValueFails() {
        assertFalse(PasswordHasher.verify("x".toCharArray(), "garbage"));
        assertFalse(PasswordHasher.verify("x".toCharArray(), "1:!!:!!"));
        assertFalse(PasswordHasher.verify("x".toCharArray(), null));
    }
}
