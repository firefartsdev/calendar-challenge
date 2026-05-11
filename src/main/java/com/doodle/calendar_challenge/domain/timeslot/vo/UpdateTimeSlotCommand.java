package com.doodle.calendar_challenge.domain.timeslot.vo;

import java.time.Instant;
import java.util.UUID;

public record UpdateTimeSlotCommand(UUID id, Instant startAt, Instant endAt, boolean busy) {
}
