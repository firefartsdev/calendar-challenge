package com.doodle.calendar_challenge.domain.meeting.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Meeting")
class MeetingTest {

    private static final UUID ID    = UUID.randomUUID();
    private static final String TITLE = "Team sync";

    @Nested
    @DisplayName("valid creation")
    class ValidCreation {

        @Test
        @DisplayName("creates with participants")
        void createsWithParticipants() {
            final var meeting = assertDoesNotThrow(() ->
                    new Meeting(ID, TITLE, "Weekly sync", Set.of("rafa", "isaac"), null));
            assertEquals(TITLE, meeting.title());
            assertEquals(2, meeting.participants().size());
        }

        @Test
        @DisplayName("creates with null participants defaulting to empty set")
        void createsWithNullParticipantsDefaultsToEmpty() {
            final var meeting = assertDoesNotThrow(() -> new Meeting(ID, TITLE, null, null, null));
            assertNotNull(meeting.participants());
            assertTrue(meeting.participants().isEmpty());
        }

        @Test
        @DisplayName("creates with null description")
        void createsWithNullDescription() {
            assertDoesNotThrow(() -> new Meeting(ID, TITLE, null, Set.of("rafa"), null));
        }
    }

    @Nested
    @DisplayName("invalid creation")
    class InvalidCreation {

        @Test
        @DisplayName("throws when title is null")
        void throwsWhenTitleIsNull() {
            assertThrows(IllegalArgumentException.class, () -> new Meeting(ID, null, null, Set.of(), null));
        }

        @Test
        @DisplayName("throws when title is blank")
        void throwsWhenTitleIsBlank() {
            assertThrows(IllegalArgumentException.class, () -> new Meeting(ID, "  ", null, Set.of(), null));
        }

        @Test
        @DisplayName("throws when participants contain a null element")
        void throwsWhenParticipantsContainNull() {
            final var participants = new java.util.HashSet<String>();
            participants.add("rafa");
            participants.add(null);
            assertThrows(IllegalArgumentException.class, () -> new Meeting(ID, TITLE, null, participants, null));
        }

        @Test
        @DisplayName("throws when participants contain a blank element")
        void throwsWhenParticipantsContainBlank() {
            assertThrows(IllegalArgumentException.class, () -> new Meeting(ID, TITLE, null, Set.of("rafa", "  "), null));
        }
    }
}
