package com.doodle.calendar_challenge.domain.timeslot.entity;

import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TimeSlot")
class TimeSlotTest {

    private static final UUID ID        = UUID.randomUUID();
    private static final String OWNER   = "rafa";
    private static final TimeRange TIME_RANGE = new TimeRange(
            Instant.parse("2025-06-01T10:00:00Z"),
            Instant.parse("2025-06-01T11:00:00Z")
    );
    private static final UUID MEETING_ID = UUID.randomUUID();

    @Nested
    @DisplayName("valid creation")
    class ValidCreation {

        @Test
        @DisplayName("creates a free slot without meeting")
        void createsFreeSlot() {
            final var slot = assertDoesNotThrow(() -> new TimeSlot(ID, OWNER, TIME_RANGE, false, null, null));
            assertFalse(slot.busy());
            assertNull(slot.meetingId());
        }

        @Test
        @DisplayName("creates a busy slot without meeting")
        void createsBusySlotWithoutMeeting() {
            assertDoesNotThrow(() -> new TimeSlot(ID, OWNER, TIME_RANGE, true, null, null));
        }

        @Test
        @DisplayName("creates a busy slot with meeting")
        void createsBusySlotWithMeeting() {
            final var slot = assertDoesNotThrow(() -> new TimeSlot(ID, OWNER, TIME_RANGE, true, MEETING_ID, null));
            assertTrue(slot.busy());
            assertEquals(MEETING_ID, slot.meetingId());
        }
    }

    @Nested
    @DisplayName("invalid creation")
    class InvalidCreation {

        @Test
        @DisplayName("throws when owner is null")
        void throwsWhenOwnerIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new TimeSlot(ID, null, TIME_RANGE, false, null, null));
        }

        @Test
        @DisplayName("throws when owner is blank")
        void throwsWhenOwnerIsBlank() {
            assertThrows(IllegalArgumentException.class, () -> new TimeSlot(ID, "  ", TIME_RANGE, false, null, null));
        }

        @Test
        @DisplayName("throws when timeRange is null")
        void throwsWhenTimeRangeIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new TimeSlot(ID, OWNER, null, false, null, null));
        }

        @Test
        @DisplayName("throws when free slot has a meetingId")
        void throwsWhenFreeSlotHasMeeting() {
            assertThrows(IllegalArgumentException.class, () -> new TimeSlot(ID, OWNER, TIME_RANGE, false, MEETING_ID, null));
        }
    }
}
