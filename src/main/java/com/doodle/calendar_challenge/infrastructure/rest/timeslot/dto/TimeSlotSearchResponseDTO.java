package com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto;

import java.util.List;

public record TimeSlotSearchResponseDTO(
        List<TimeSlotResponseDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
