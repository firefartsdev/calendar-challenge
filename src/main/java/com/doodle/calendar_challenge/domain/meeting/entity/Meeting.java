package com.doodle.calendar_challenge.domain.meeting.entity;

import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;

import java.util.Set;
import java.util.UUID;

public record Meeting(UUID id, String title, String organizer, Set<String> participants, TimeRange timeRange, Long version) {

    public Meeting {
        if(title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }

        if(organizer == null || organizer.isBlank()) {
            throw new IllegalArgumentException("Organizer cannot be null or empty");
        }

        if(timeRange == null) {
            throw new IllegalArgumentException("Time range cannot be null");
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
