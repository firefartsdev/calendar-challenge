package com.doodle.calendar_challenge.application.exception;

public class OverlappingTimeSlotException extends RuntimeException {

    public OverlappingTimeSlotException(String message) {
        super(message);
    }
}
