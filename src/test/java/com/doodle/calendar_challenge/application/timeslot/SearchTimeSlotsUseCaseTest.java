package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.SearchTimeSlotsQuery;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchTimeSlotsUseCase")
class SearchTimeSlotsUseCaseTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private SearchTimeSlotsUseCase useCase;

    private static final String OWNER_A = "alice";
    private static final String OWNER_B = "bob";
    private static final TimeRange RANGE = new TimeRange(
            Instant.parse("2025-06-01T08:00:00Z"),
            Instant.parse("2025-06-01T18:00:00Z"));

    private TimeSlot freeSlot(String owner) {
        return new TimeSlot(UUID.randomUUID(), owner,
                new TimeRange(Instant.parse("2025-06-01T09:00:00Z"), Instant.parse("2025-06-01T10:00:00Z")),
                false, null, null);
    }

    private TimeSlot busySlot(String owner) {
        return new TimeSlot(UUID.randomUUID(), owner,
                new TimeRange(Instant.parse("2025-06-01T11:00:00Z"), Instant.parse("2025-06-01T12:00:00Z")),
                true, UUID.randomUUID(), null);
    }

    @Test
    @DisplayName("returns all slots for multiple owners when busy filter is null")
    void returnsAllSlotsForMultipleOwners() {
        final var slotA = freeSlot(OWNER_A);
        final var slotB = busySlot(OWNER_B);
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A, OWNER_B), RANGE, null);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A, OWNER_B), RANGE, null))
                .thenReturn(List.of(slotA, slotB));

        final var result = useCase.searchTimeSlots(query);

        assertEquals(2, result.size());
        verify(timeSlotRepository).searchByOwnersAndTimeRange(List.of(OWNER_A, OWNER_B), RANGE, null);
    }

    @Test
    @DisplayName("passes busy=true filter to repository when searching only busy slots")
    void filtersBusySlots() {
        final var slotA = busySlot(OWNER_A);
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A, OWNER_B), RANGE, true);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A, OWNER_B), RANGE, true))
                .thenReturn(List.of(slotA));

        final var result = useCase.searchTimeSlots(query);

        assertEquals(1, result.size());
        assertTrue(result.get(0).busy());
    }

    @Test
    @DisplayName("passes busy=false filter to repository when searching only free slots")
    void filtersFreeSlots() {
        final var slotA = freeSlot(OWNER_A);
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A, OWNER_B), RANGE, false);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A, OWNER_B), RANGE, false))
                .thenReturn(List.of(slotA));

        final var result = useCase.searchTimeSlots(query);

        assertEquals(1, result.size());
        assertFalse(result.get(0).busy());
    }

    @Test
    @DisplayName("returns empty list when no slots match")
    void returnsEmptyWhenNoMatch() {
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A), RANGE, null);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A), RANGE, null))
                .thenReturn(List.of());

        assertTrue(useCase.searchTimeSlots(query).isEmpty());
    }

    @Test
    @DisplayName("throws IllegalArgumentException when owners list is empty")
    void throwsWhenOwnersEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchTimeSlotsQuery(List.of(), RANGE, null));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when owners list is null")
    void throwsWhenOwnersNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchTimeSlotsQuery(null, RANGE, null));
    }
}
