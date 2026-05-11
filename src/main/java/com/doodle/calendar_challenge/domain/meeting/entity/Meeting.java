package com.doodle.calendar_challenge.domain.meeting.entity;

import java.util.Set;
import java.util.UUID;

public record Meeting(UUID id, String title, String description, Set<String> participants, Long version) {

    public Meeting {
        if(title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }

        participants = participants == null ? Set.of() : participants;

        if(participants.contains(null)) {
            throw new IllegalArgumentException("Participant cannot be null");
        }

        if(participants.stream()
                .anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Participant cannot be empty");
        }
    }
}
