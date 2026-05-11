package com.doodle.calendar_challenge.domain.timeslot.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeRange")
class TimeRangeTest {

    private static final Instant START = Instant.parse("2025-06-01T10:00:00Z");
    private static final Instant END   = Instant.parse("2025-06-01T11:00:00Z");

    @Nested
    @DisplayName("valid creation")
    class ValidCreation {

        @Test
        @DisplayName("creates when startAt is before endAt")
        void createsWhenStartBeforeEnd() {
            final var timeRange = assertDoesNotThrow(() -> new TimeRange(START, END));
            assertEquals(START, timeRange.startAt());
            assertEquals(END, timeRange.endAt());
        }
    }

    @Nested
    @DisplayName("invalid creation")
    class InvalidCreation {

        @Test
        @DisplayName("throws when startAt is null")
        void throwsWhenStartAtIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new TimeRange(null, END));
        }

        @Test
        @DisplayName("throws when endAt is null")
        void throwsWhenEndAtIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new TimeRange(START, null));
        }

        @Test
        @DisplayName("throws when startAt equals endAt")
        void throwsWhenStartEqualsEnd() {
            assertThrows(IllegalArgumentException.class, () -> new TimeRange(START, START));
        }

        @Test
        @DisplayName("throws when startAt is after endAt")
        void throwsWhenStartAfterEnd() {
            assertThrows(IllegalArgumentException.class, () -> new TimeRange(END, START));
        }
    }
}
