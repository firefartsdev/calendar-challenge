package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListTimeSlotsByOwnerUseCase")
class ListTimeSlotsByOwnerUseCaseTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private ListTimeSlotsByOwnerUseCase useCase;

    private static final String OWNER = "rafa";

    @Test
    @DisplayName("returns slots ordered by startAt from repository")
    void returnsOrderedSlots() {
        final var slot1 = new TimeSlot(UUID.randomUUID(), OWNER,
                new TimeRange(Instant.parse("2025-06-01T09:00:00Z"), Instant.parse("2025-06-01T10:00:00Z")),
                false, null, null);
        final var slot2 = new TimeSlot(UUID.randomUUID(), OWNER,
                new TimeRange(Instant.parse("2025-06-01T11:00:00Z"), Instant.parse("2025-06-01T12:00:00Z")),
                false, null, null);
        when(timeSlotRepository.findByOwnerOrderByStartAt(OWNER)).thenReturn(List.of(slot1, slot2));

        final var result = useCase.listTimeSlotsByOwner(OWNER);

        assertEquals(2, result.size());
        assertEquals(slot1.id(), result.get(0).id());
        assertEquals(slot2.id(), result.get(1).id());
    }

    @Test
    @DisplayName("returns empty list when owner has no slots")
    void returnsEmptyListWhenNoSlots() {
        when(timeSlotRepository.findByOwnerOrderByStartAt(OWNER)).thenReturn(List.of());

        assertTrue(useCase.listTimeSlotsByOwner(OWNER).isEmpty());
    }
}
