package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.application.exception.OverlappingTimeSlotException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFoundException;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import com.doodle.calendar_challenge.domain.timeslot.vo.UpdateTimeSlotCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateTimeSlotUseCase")
class UpdateTimeSlotUseCaseTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @InjectMocks
    private UpdateTimeSlotUseCase useCase;

    private static final UUID   SLOT_ID    = UUID.randomUUID();
    private static final UUID   MEETING_ID = UUID.randomUUID();
    private static final String OWNER      = "rafa";
    private static final TimeRange EXISTING_RANGE = new TimeRange(
            Instant.parse("2025-06-01T10:00:00Z"),
            Instant.parse("2025-06-01T11:00:00Z")
    );
    private static final TimeSlot EXISTING_SLOT = new TimeSlot(SLOT_ID, OWNER, EXISTING_RANGE, false, null, 1L);

    @Nested
    @DisplayName("successful update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("saves updated slot preserving owner, meetingId and version")
        void savesUpdatedSlotPreservingImmutableFields() {
            final var existingWithMeeting = new TimeSlot(SLOT_ID, OWNER, EXISTING_RANGE, true, MEETING_ID, 2L);
            final var newStart = Instant.parse("2025-06-01T12:00:00Z");
            final var newEnd   = Instant.parse("2025-06-01T13:00:00Z");
            final var command  = new UpdateTimeSlotCommand(SLOT_ID, newStart, newEnd, true);

            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(existingWithMeeting));
            when(timeSlotRepository.existsOverlappingSlotExcluding(eq(OWNER), any(TimeRange.class), eq(SLOT_ID))).thenReturn(false);

            final var captor = ArgumentCaptor.forClass(TimeSlot.class);
            when(timeSlotRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

            useCase.updateTimeSlot(command);

            final var saved = captor.getValue();
            assertEquals(SLOT_ID, saved.id());
            assertEquals(OWNER, saved.owner());
            assertEquals(newStart, saved.timeRange().startAt());
            assertEquals(newEnd, saved.timeRange().endAt());
            assertEquals(MEETING_ID, saved.meetingId());
            assertEquals(2L, saved.version());
        }
    }

    @Nested
    @DisplayName("failed update")
    class FailedUpdate {

        @Test
        @DisplayName("throws TimeSlotNotFoundException when slot does not exist")
        void throwsWhenSlotNotFound() {
            final var command = new UpdateTimeSlotCommand(SLOT_ID,
                    Instant.parse("2025-06-01T12:00:00Z"), Instant.parse("2025-06-01T13:00:00Z"), false);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());

            assertThrows(TimeSlotNotFoundException.class, () -> useCase.updateTimeSlot(command));
            verify(timeSlotRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws OverlappingTimeSlotException when new range overlaps another slot")
        void throwsWhenOverlapExists() {
            final var command = new UpdateTimeSlotCommand(SLOT_ID,
                    Instant.parse("2025-06-01T12:00:00Z"), Instant.parse("2025-06-01T13:00:00Z"), false);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(EXISTING_SLOT));
            when(timeSlotRepository.existsOverlappingSlotExcluding(eq(OWNER), any(TimeRange.class), eq(SLOT_ID))).thenReturn(true);

            assertThrows(OverlappingTimeSlotException.class, () -> useCase.updateTimeSlot(command));
            verify(timeSlotRepository, never()).save(any());
        }
    }
}
