package com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record TimeSlotSearchRequestDTO(
        @NotEmpty @Size(max = 50) List<String> owners,
        @NotNull Instant startAt,
        @NotNull Instant endAt,
        Boolean busy,
        Integer page,
        Integer size) {
}
