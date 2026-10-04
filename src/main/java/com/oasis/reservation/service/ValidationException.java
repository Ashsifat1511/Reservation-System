package com.oasis.reservation.service;

import java.util.List;

/** Thrown by the service layer when booking input is invalid. Carries user-facing messages. */
public class ValidationException extends Exception {

    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public List<String> errors() {
        return errors;
    }
}
