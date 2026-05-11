package com.doodle.calendar_challenge.domain;

import java.util.UUID;

public record TimeSlot(UUID id, String owner, TimeRange timeRange, boolean busy, UUID meetingId, Long version) {

    public TimeSlot {
        if(owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Owner is required");
        }

        if(timeRange == null) {
            throw new IllegalArgumentException("Time range is required");
        }

        if(busy  && meetingId == null) {
            throw new IllegalArgumentException("Busy time slot must have a meeting id");
        }

        if(!busy && meetingId != null) {
            throw new IllegalArgumentException("Free slot cannot have a meeting id");
        }
    }
}
