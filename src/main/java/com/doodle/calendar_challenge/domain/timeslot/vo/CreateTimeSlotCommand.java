package com.doodle.calendar_challenge.domain.timeslot.vo;

import java.time.Instant;

public record CreateTimeSlotCommand(String owner, Instant startAt, Instant endAt, boolean busy) {
}
