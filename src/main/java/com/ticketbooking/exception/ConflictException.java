package com.ticketbooking.exception;

/** The request is valid but clashes with current state, e.g. the seat is already booked. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
