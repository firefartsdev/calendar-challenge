package com.doodle.calendar_challenge.infrastructure.persistence.meeting;

import com.doodle.calendar_challenge.TestcontainersConfiguration;
import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import com.doodle.calendar_challenge.domain.meeting.port.MeetingRepository;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
@DisplayName("MeetingJPARepositoryAdapter")
class MeetingJPARepositoryAdapterTest {

    @Autowired
    private MeetingRepository adapter;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists and returns domain object with participants")
        void persistsAndReturnsDomainObject() {
            final var meeting = buildMeeting("Sprint Planning", Set.of("alice", "bob"));

            final var saved = adapter.save(meeting);

            assertEquals(meeting.id(), saved.id());
            assertEquals("Sprint Planning", saved.title());
            assertEquals(Set.of("alice", "bob"), saved.participants());
            assertNotNull(saved.version());
        }

        @Test
        @DisplayName("persists meeting with empty participants")
        void persistsMeetingWithNoParticipants() {
            final var meeting = buildMeeting("Solo Event", Set.of());

            final var saved = adapter.save(meeting);

            assertEquals(meeting.id(), saved.id());
            assertTrue(saved.participants().isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns meeting when it exists")
        void returnsMeetingWhenExists() {
            final var saved = adapter.save(buildMeeting("Team Sync", Set.of("alice")));

            final var found = adapter.findById(saved.id());

            assertTrue(found.isPresent());
            assertEquals(saved.id(), found.get().id());
            assertEquals("Team Sync", found.get().title());
        }

        @Test
        @DisplayName("returns empty when meeting does not exist")
        void returnsEmptyWhenNotFound() {
            assertTrue(adapter.findById(UUID.randomUUID()).isEmpty());
        }
    }

    @Nested
    @DisplayName("findAllById")
    class FindAllById {

        @Test
        @DisplayName("returns all meetings matching the given ids")
        void returnsAllMatchingMeetings() {
            final var m1 = adapter.save(buildMeeting("Meeting 1", Set.of("alice")));
            final var m2 = adapter.save(buildMeeting("Meeting 2", Set.of("bob")));
            adapter.save(buildMeeting("Meeting 3", Set.of("carol")));

            final var result = adapter.findAllById(Set.of(m1.id(), m2.id()));

            assertEquals(2, result.size());
            assertTrue(result.stream().anyMatch(m -> m.id().equals(m1.id())));
            assertTrue(result.stream().anyMatch(m -> m.id().equals(m2.id())));
        }

        @Test
        @DisplayName("returns empty list when no ids match")
        void returnsEmptyListWhenNoMatch() {
            assertTrue(adapter.findAllById(Set.of(UUID.randomUUID())).isEmpty());
        }
    }

    private Meeting buildMeeting(String title, Set<String> participants) {
        return new Meeting(
                Generators.timeBasedEpochGenerator().generate(),
                title,
                "Test description",
                participants,
                null);
    }
}
