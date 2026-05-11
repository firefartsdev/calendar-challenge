package com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateTimeSlotRequestDTO(@NotBlank String owner, @NotNull Instant startAt, @NotNull Instant endAt) {

}
