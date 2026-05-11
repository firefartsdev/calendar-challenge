package com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateTimeSlotRequestDTO(@NotNull Instant startAt, @NotNull Instant endAt, boolean busy) {
}
