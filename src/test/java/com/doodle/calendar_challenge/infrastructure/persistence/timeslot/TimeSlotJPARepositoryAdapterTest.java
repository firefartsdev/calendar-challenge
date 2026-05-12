package com.doodle.calendar_challenge.infrastructure.persistence.timeslot;

import com.doodle.calendar_challenge.TestcontainersConfiguration;
import com.doodle.calendar_challenge.application.exception.OverlappingTimeSlotException;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("TimeSlotJPARepositoryAdapter")
class TimeSlotJPARepositoryAdapterTest {

    @Autowired
    private TimeSlotRepository adapter;

    @Autowired
    private TimeSlotJPARepository repository;

    private static final String OWNER = "test-alice";
    private static final String OTHER_OWNER = "test-bob";
    private static final Instant BASE = Instant.parse("2030-01-01T09:00:00Z");

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists and returns domain object with version")
        void persistsAndReturnsDomainObject() {
            final var slot = buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false);
            final var saved = adapter.save(slot);

            assertEquals(slot.id(), saved.id());
            assertEquals(OWNER, saved.owner());
            assertFalse(saved.busy());
            assertNotNull(saved.version());
        }

        @Test
        @DisplayName("throws OverlappingTimeSlotException when exclusion constraint fires")
        void throwsWhenConstraintViolated() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));
            final var overlapping = buildSlot(OWNER, BASE.plusSeconds(1800), BASE.plusSeconds(5400), false);

            assertThrows(OverlappingTimeSlotException.class, () -> adapter.save(overlapping));
        }

        @Test
        @DisplayName("allows adjacent slots for the same owner")
        void allowsAdjacentSlotsForSameOwner() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            assertDoesNotThrow(() ->
                    adapter.save(buildSlot(OWNER, BASE.plusSeconds(3600), BASE.plusSeconds(7200), false)));
        }

        @Test
        @DisplayName("allows overlapping slots for different owners")
        void allowsOverlappingForDifferentOwners() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            assertDoesNotThrow(() ->
                    adapter.save(buildSlot(OTHER_OWNER, BASE, BASE.plusSeconds(3600), false)));
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("returns slot when it exists")
        void returnsSlotWhenExists() {
            final var saved = adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            final var found = adapter.findById(saved.id());

            assertTrue(found.isPresent());
            assertEquals(saved.id(), found.get().id());
        }

        @Test
        @DisplayName("returns empty when slot does not exist")
        void returnsEmptyWhenNotFound() {
            assertTrue(adapter.findById(UUID.randomUUID()).isEmpty());
        }
    }

    @Nested
    @DisplayName("existsOverlappingSlot")
    class ExistsOverlappingSlot {

        @Test
        @DisplayName("returns true when ranges overlap")
        void returnsTrueWhenOverlapExists() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            assertTrue(adapter.existsOverlappingSlot(OWNER,
                    new TimeRange(BASE.plusSeconds(1800), BASE.plusSeconds(5400))));
        }

        @Test
        @DisplayName("returns false for adjacent slots")
        void returnsFalseForAdjacentSlots() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            assertFalse(adapter.existsOverlappingSlot(OWNER,
                    new TimeRange(BASE.plusSeconds(3600), BASE.plusSeconds(7200))));
        }

        @Test
        @DisplayName("returns false for a different owner")
        void returnsFalseForDifferentOwner() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            assertFalse(adapter.existsOverlappingSlot(OTHER_OWNER,
                    new TimeRange(BASE, BASE.plusSeconds(3600))));
        }
    }

    @Nested
    @DisplayName("existsOverlappingSlotExcluding")
    class ExistsOverlappingSlotExcluding {

        @Test
        @DisplayName("returns false when the only overlapping slot is the excluded one")
        void returnsFalseWhenOnlyOverlapIsExcluded() {
            final var saved = adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            assertFalse(adapter.existsOverlappingSlotExcluding(OWNER,
                    new TimeRange(BASE, BASE.plusSeconds(3600)), saved.id()));
        }

        @Test
        @DisplayName("returns true when a non-excluded slot overlaps")
        void returnsTrueWhenNonExcludedSlotOverlaps() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            assertTrue(adapter.existsOverlappingSlotExcluding(OWNER,
                    new TimeRange(BASE, BASE.plusSeconds(3600)), UUID.randomUUID()));
        }
    }

    @Nested
    @DisplayName("findFreeSlotCovering")
    class FindFreeSlotCovering {

        @Test
        @DisplayName("returns slot when a free slot fully covers the range")
        void returnsSlotWhenCoversRange() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(7200), false));

            final var result = adapter.findFreeSlotCovering(OWNER,
                    new TimeRange(BASE.plusSeconds(1800), BASE.plusSeconds(5400)));

            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("returns empty when the covering slot is busy")
        void returnsEmptyWhenSlotIsBusy() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(7200), true));

            assertTrue(adapter.findFreeSlotCovering(OWNER,
                    new TimeRange(BASE.plusSeconds(1800), BASE.plusSeconds(5400))).isEmpty());
        }

        @Test
        @DisplayName("returns empty when no slot fully covers the range")
        void returnsEmptyWhenNoSlotCoversRange() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(1800), false));

            assertTrue(adapter.findFreeSlotCovering(OWNER,
                    new TimeRange(BASE, BASE.plusSeconds(3600))).isEmpty());
        }
    }

    @Nested
    @DisplayName("findFreeSlotsCoveringForOwners")
    class FindFreeSlotsCoveringForOwners {

        @Test
        @DisplayName("returns one entry per owner with a covering free slot")
        void returnsOneEntryPerOwner() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(7200), false));
            adapter.save(buildSlot(OTHER_OWNER, BASE, BASE.plusSeconds(7200), false));

            final var result = adapter.findFreeSlotsCoveringForOwners(
                    Set.of(OWNER, OTHER_OWNER),
                    new TimeRange(BASE.plusSeconds(1800), BASE.plusSeconds(5400)));

            assertEquals(2, result.size());
            assertTrue(result.containsKey(OWNER));
            assertTrue(result.containsKey(OTHER_OWNER));
        }

        @Test
        @DisplayName("excludes owners without a covering free slot")
        void excludesOwnersWithoutCoveringSlot() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(7200), false));

            final var result = adapter.findFreeSlotsCoveringForOwners(
                    Set.of(OWNER, OTHER_OWNER),
                    new TimeRange(BASE.plusSeconds(1800), BASE.plusSeconds(5400)));

            assertEquals(1, result.size());
            assertTrue(result.containsKey(OWNER));
        }

        @Test
        @DisplayName("excludes busy slots")
        void excludesBusySlots() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(7200), true));

            final var result = adapter.findFreeSlotsCoveringForOwners(
                    Set.of(OWNER),
                    new TimeRange(BASE.plusSeconds(1800), BASE.plusSeconds(5400)));

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("markSlotsAsBusy")
    class MarkSlotsAsBusy {

        @Test
        @DisplayName("marks specified slots as busy and returns the updated count")
        void marksAsBusyAndReturnsCount() {
            final var slot1 = adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));
            final var slot2 = adapter.save(buildSlot(OTHER_OWNER, BASE, BASE.plusSeconds(3600), false));
            final var meetingId = UUID.randomUUID();

            final int updated = adapter.markSlotsAsBusy(List.of(slot1.id(), slot2.id()), meetingId);

            assertEquals(2, updated);
            assertTrue(adapter.findById(slot1.id()).get().busy());
            assertTrue(adapter.findById(slot2.id()).get().busy());
        }

        @Test
        @DisplayName("skips already-busy slots and returns 0")
        void skipsAlreadyBusySlots() {
            final var slot = adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), true));

            final int updated = adapter.markSlotsAsBusy(List.of(slot.id()), UUID.randomUUID());

            assertEquals(0, updated);
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("removes the slot so it can no longer be found")
        void removesSlot() {
            final var saved = adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));

            adapter.deleteById(saved.id());

            assertTrue(adapter.findById(saved.id()).isEmpty());
        }
    }

    @Nested
    @DisplayName("searchByOwnersAndTimeRange")
    class SearchByOwnersAndTimeRange {

        @Test
        @DisplayName("returns all slots within the time range for the given owners")
        void returnsAllSlotsInRange() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));
            adapter.save(buildSlot(OWNER, BASE.plusSeconds(7200), BASE.plusSeconds(10800), false));
            adapter.save(buildSlot(OTHER_OWNER, BASE, BASE.plusSeconds(3600), true));

            final var result = adapter.searchByOwnersAndTimeRange(
                    List.of(OWNER, OTHER_OWNER),
                    new TimeRange(BASE.minusSeconds(1), BASE.plusSeconds(11000)),
                    null, 0, 10);

            assertEquals(3, result.totalElements());
            assertEquals(3, result.content().size());
        }

        @Test
        @DisplayName("filters by busy flag when provided")
        void filtersByBusy() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));
            adapter.save(buildSlot(OTHER_OWNER, BASE, BASE.plusSeconds(3600), true));

            final var result = adapter.searchByOwnersAndTimeRange(
                    List.of(OWNER, OTHER_OWNER),
                    new TimeRange(BASE.minusSeconds(1), BASE.plusSeconds(3601)),
                    true, 0, 10);

            assertEquals(1, result.totalElements());
            assertTrue(result.content().get(0).busy());
        }

        @Test
        @DisplayName("respects page size and reports correct totalPages")
        void respectsPageSizeAndTotalPages() {
            for (int i = 0; i < 5; i++) {
                adapter.save(buildSlot("test-user-" + i, BASE, BASE.plusSeconds(3600), false));
            }

            final var result = adapter.searchByOwnersAndTimeRange(
                    List.of("test-user-0", "test-user-1", "test-user-2", "test-user-3", "test-user-4"),
                    new TimeRange(BASE.minusSeconds(1), BASE.plusSeconds(3601)),
                    null, 0, 2);

            assertEquals(5, result.totalElements());
            assertEquals(2, result.content().size());
            assertEquals(3, result.totalPages());
        }

        @Test
        @DisplayName("excludes slots outside the requested time range")
        void excludesOutsideTimeRange() {
            adapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600), false));
            adapter.save(buildSlot(OWNER, BASE.plusSeconds(10000), BASE.plusSeconds(14000), false));

            final var result = adapter.searchByOwnersAndTimeRange(
                    List.of(OWNER),
                    new TimeRange(BASE.minusSeconds(1), BASE.plusSeconds(3601)),
                    null, 0, 10);

            assertEquals(1, result.totalElements());
        }
    }

    private TimeSlot buildSlot(String owner, Instant start, Instant end, boolean busy) {
        return new TimeSlot(
                Generators.timeBasedEpochGenerator().generate(),
                owner,
                new TimeRange(start, end),
                busy,
                null,
                null);
    }
}
