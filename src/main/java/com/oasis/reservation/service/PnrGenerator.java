package com.oasis.reservation.service;

import java.security.SecureRandom;

/** Generates 10-digit PNR numbers (first digit never 0). Uniqueness is enforced by the caller. */
public class PnrGenerator {

    public static final int PNR_LENGTH = 10;
    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder sb = new StringBuilder(PNR_LENGTH);
        sb.append(1 + random.nextInt(9));
        for (int i = 1; i < PNR_LENGTH; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public static boolean isWellFormed(String pnr) {
        return pnr != null && pnr.matches("[1-9]\\d{" + (PNR_LENGTH - 1) + "}");
    }
}
