package com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto;

import java.time.Instant;
import java.util.UUID;

public record TimeSlotResponseDTO(UUID id, String owner, Instant startAt, Instant endAt, boolean busy) {
}
