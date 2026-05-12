package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.domain.shared.PagedResult;
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

    private PagedResult<TimeSlot> pageOf(TimeSlot... slots) {
        return new PagedResult<>(List.of(slots), slots.length, 1, 0, 20);
    }

    @Test
    @DisplayName("returns all slots for multiple owners when busy filter is null")
    void returnsAllSlotsForMultipleOwners() {
        final var slotA = freeSlot(OWNER_A);
        final var slotB = busySlot(OWNER_B);
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A, OWNER_B), RANGE, null, 0, 20);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A, OWNER_B), RANGE, null, 0, 20))
                .thenReturn(pageOf(slotA, slotB));

        final var result = useCase.searchTimeSlots(query);

        assertEquals(2, result.content().size());
        assertEquals(2, result.totalElements());
        verify(timeSlotRepository).searchByOwnersAndTimeRange(List.of(OWNER_A, OWNER_B), RANGE, null, 0, 20);
    }

    @Test
    @DisplayName("passes busy=true filter to repository when searching only busy slots")
    void filtersBusySlots() {
        final var slotA = busySlot(OWNER_A);
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A, OWNER_B), RANGE, true, 0, 20);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A, OWNER_B), RANGE, true, 0, 20))
                .thenReturn(pageOf(slotA));

        final var result = useCase.searchTimeSlots(query);

        assertEquals(1, result.content().size());
        assertTrue(result.content().get(0).busy());
    }

    @Test
    @DisplayName("passes busy=false filter to repository when searching only free slots")
    void filtersFreeSlots() {
        final var slotA = freeSlot(OWNER_A);
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A, OWNER_B), RANGE, false, 0, 20);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A, OWNER_B), RANGE, false, 0, 20))
                .thenReturn(pageOf(slotA));

        final var result = useCase.searchTimeSlots(query);

        assertEquals(1, result.content().size());
        assertFalse(result.content().get(0).busy());
    }

    @Test
    @DisplayName("returns empty page when no slots match")
    void returnsEmptyWhenNoMatch() {
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A), RANGE, null, 0, 20);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A), RANGE, null, 0, 20))
                .thenReturn(new PagedResult<>(List.of(), 0, 0, 0, 20));

        final var result = useCase.searchTimeSlots(query);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    @DisplayName("propagates pagination parameters to repository")
    void propagatesPaginationParams() {
        final var slotA = freeSlot(OWNER_A);
        final var query = new SearchTimeSlotsQuery(List.of(OWNER_A), RANGE, null, 2, 10);

        when(timeSlotRepository.searchByOwnersAndTimeRange(List.of(OWNER_A), RANGE, null, 2, 10))
                .thenReturn(new PagedResult<>(List.of(slotA), 25, 3, 2, 10));

        final var result = useCase.searchTimeSlots(query);

        assertEquals(2, result.currentPage());
        assertEquals(10, result.pageSize());
        assertEquals(25, result.totalElements());
        assertEquals(3, result.totalPages());
    }

    @Test
    @DisplayName("throws IllegalArgumentException when owners list is empty")
    void throwsWhenOwnersEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchTimeSlotsQuery(List.of(), RANGE, null, 0, 20));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when owners list is null")
    void throwsWhenOwnersNull() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchTimeSlotsQuery(null, RANGE, null, 0, 20));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when page is negative")
    void throwsWhenPageNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchTimeSlotsQuery(List.of(OWNER_A), RANGE, null, -1, 20));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when size exceeds maximum")
    void throwsWhenSizeExceedsMax() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchTimeSlotsQuery(List.of(OWNER_A), RANGE, null, 0, 101));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when size is zero")
    void throwsWhenSizeIsZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new SearchTimeSlotsQuery(List.of(OWNER_A), RANGE, null, 0, 0));
    }

    @Test
    @DisplayName("throws IllegalArgumentException when owners exceed 50")
    void throwsWhenOwnersTooMany() {
        final var tooManyOwners = java.util.stream.IntStream.rangeClosed(1, 51)
                .mapToObj(i -> "user" + i)
                .toList();
        assertThrows(IllegalArgumentException.class,
                () -> new SearchTimeSlotsQuery(tooManyOwners, RANGE, null, 0, 20));
    }

    @Test
    @DisplayName("accepts exactly 50 owners")
    void accepts50Owners() {
        final var fiftyOwners = java.util.stream.IntStream.rangeClosed(1, 50)
                .mapToObj(i -> "user" + i)
                .toList();
        assertDoesNotThrow(() -> new SearchTimeSlotsQuery(fiftyOwners, RANGE, null, 0, 20));
    }
}
