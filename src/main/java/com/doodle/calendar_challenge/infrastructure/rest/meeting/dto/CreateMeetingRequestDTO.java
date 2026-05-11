package com.doodle.calendar_challenge.infrastructure.rest.meeting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record CreateMeetingRequestDTO(
        @NotNull UUID timeSlotId,
        @NotBlank String title,
        String description,
        Set<String> participants
) {
}
