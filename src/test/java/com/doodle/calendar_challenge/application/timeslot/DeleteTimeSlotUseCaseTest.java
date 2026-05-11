package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.application.exception.TimeSlotHasMeetingException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFoundException;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteTimeSlotUseCase")
class DeleteTimeSlotUseCaseTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private DeleteTimeSlotUseCase useCase;

    private static final UUID   SLOT_ID    = UUID.randomUUID();
    private static final UUID   MEETING_ID = UUID.randomUUID();
    private static final String OWNER      = "rafa";
    private static final TimeRange TIME_RANGE = new TimeRange(
            Instant.parse("2025-06-01T10:00:00Z"),
            Instant.parse("2025-06-01T11:00:00Z")
    );

    @Nested
    @DisplayName("successful deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("deletes when slot exists and has no meeting")
        void deletesWhenSlotFree() {
            final var freeSlot = new TimeSlot(SLOT_ID, OWNER, TIME_RANGE, false, null, 1L);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(freeSlot));

            useCase.deleteTimeSlot(SLOT_ID);

            verify(timeSlotRepository).deleteById(SLOT_ID);
        }
    }

    @Nested
    @DisplayName("failed deletion")
    class FailedDeletion {

        @Test
        @DisplayName("throws TimeSlotNotFoundException when slot does not exist")
        void throwsWhenSlotNotFound() {
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

            assertThrows(TimeSlotNotFoundException.class, () -> useCase.deleteTimeSlot(SLOT_ID));
            verify(timeSlotRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("throws TimeSlotHasMeetingException when slot has a meeting assigned")
        void throwsWhenSlotHasMeeting() {
            final var busySlot = new TimeSlot(SLOT_ID, OWNER, TIME_RANGE, true, MEETING_ID, 1L);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(busySlot));

            assertThrows(TimeSlotHasMeetingException.class, () -> useCase.deleteTimeSlot(SLOT_ID));
            verify(timeSlotRepository, never()).deleteById(any());
        }
    }
}
