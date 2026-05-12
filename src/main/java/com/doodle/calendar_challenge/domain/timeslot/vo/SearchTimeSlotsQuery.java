package com.doodle.calendar_challenge.domain.timeslot.vo;

import java.util.List;
import java.util.Objects;

public record SearchTimeSlotsQuery(List<String> owners, TimeRange timeRange, Boolean busy) {

    public SearchTimeSlotsQuery {
        if (owners == null || owners.isEmpty()) {
            throw new IllegalArgumentException("owners must not be empty");
        }
        Objects.requireNonNull(timeRange, "timeRange must not be null");
    }
}
