package com.doodle.calendar_challenge.application.exception;

import java.util.UUID;

public class TimeSlotHasMeetingException extends RuntimeException {

    public TimeSlotHasMeetingException(UUID id) {
        super("TimeSlot with id=" + id + " cannot be deleted because it has a meeting assigned");
    }
}
