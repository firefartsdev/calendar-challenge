package com.doodle.calendar_challenge.application.exception;

import java.util.UUID;

public class TimeSlotNotFoundException extends RuntimeException {

    public TimeSlotNotFoundException(UUID id) {
        super("TimeSlot with id=" + id + " not found");
    }
}
