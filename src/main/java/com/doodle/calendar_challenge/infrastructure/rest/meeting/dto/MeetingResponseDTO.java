package com.doodle.calendar_challenge.infrastructure.rest.meeting.dto;

import java.util.Set;
import java.util.UUID;

public record MeetingResponseDTO(UUID id, String title, String description, Set<String> participants) {
}
