package com.doodle.calendar_challenge.domain.timeslot.vo;

import java.time.Instant;

public record TimeRange(Instant startAt, Instant endAt) {

    public TimeRange {
        if(startAt == null || endAt == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if(!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("Start date should be before end date");
        }
    }

}
