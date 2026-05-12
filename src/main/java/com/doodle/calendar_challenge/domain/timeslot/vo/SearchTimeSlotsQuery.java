package com.doodle.calendar_challenge.domain.timeslot.vo;

import java.util.List;
import java.util.Objects;

public record SearchTimeSlotsQuery(List<String> owners, TimeRange timeRange, Boolean busy, int page, int size) {

    public SearchTimeSlotsQuery {
        if (owners == null || owners.isEmpty()) {
            throw new IllegalArgumentException("owners must not be empty");
        }
        if (owners.size() > 50) {
            throw new IllegalArgumentException("owners must not exceed 50");
        }
        Objects.requireNonNull(timeRange, "timeRange must not be null");
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }
}
