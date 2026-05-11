package com.doodle.calendar_challenge.application.exception;

import java.util.UUID;

public class TimeSlotNotFreeException extends RuntimeException {

    public TimeSlotNotFreeException(UUID timeSlotId) {
        super("TimeSlot with id=" + timeSlotId + " is not free");
    }
}
