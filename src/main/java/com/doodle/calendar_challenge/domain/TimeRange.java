package com.doodle.calendar_challenge.domain;

import java.time.Instant;

public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if(start == null || end == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if(!start.isBefore(end)) {
            throw new IllegalArgumentException("Start date should be before end date");
        }
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }
}
