package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.application.exception.OverlappingTimeSlotException;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.CreateTimeSlotCommand;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTimeSlotUseCase")
class CreateTimeSlotUseCaseTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    private CreateTimeSlotUseCase useCase;

    private static final String OWNER = "rafa";
    private static final Instant START = Instant.parse("2025-06-01T10:00:00Z");
    private static final Instant END   = Instant.parse("2025-06-01T11:00:00Z");

    @BeforeEach
    void setUp() {
        useCase = new CreateTimeSlotUseCase(timeSlotRepository, new SimpleMeterRegistry());
    }

    @Nested
    @DisplayName("successful creation")
    class SuccessfulCreation {

        @Test
        @DisplayName("saves and returns the new time slot")
        void savesAndReturnsNewTimeSlot() {
            final var command = new CreateTimeSlotCommand(OWNER, START, END, false);
            when(timeSlotRepository.existsOverlappingSlot(eq(OWNER), any(TimeRange.class))).thenReturn(false);

            final var captor = ArgumentCaptor.forClass(TimeSlot.class);
            when(timeSlotRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

            final var result = useCase.createTimeSlot(command);

            assertNotNull(result.id());
            assertEquals(OWNER, result.owner());
            assertEquals(START, result.timeRange().startAt());
            assertEquals(END, result.timeRange().endAt());
            assertFalse(result.busy());
            assertNull(result.meetingId());
        }

        @Test
        @DisplayName("saves a busy slot when busy flag is true")
        void savesBusySlot() {
            final var command = new CreateTimeSlotCommand(OWNER, START, END, true);
            when(timeSlotRepository.existsOverlappingSlot(eq(OWNER), any(TimeRange.class))).thenReturn(false);
            when(timeSlotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            final var result = useCase.createTimeSlot(command);

            assertTrue(result.busy());
        }
    }

    @Nested
    @DisplayName("failed creation")
    class FailedCreation {

        @Test
        @DisplayName("throws OverlappingTimeSlotException when overlap exists")
        void throwsWhenOverlapExists() {
            final var command = new CreateTimeSlotCommand(OWNER, START, END, false);
            when(timeSlotRepository.existsOverlappingSlot(eq(OWNER), any(TimeRange.class))).thenReturn(true);

            assertThrows(OverlappingTimeSlotException.class, () -> useCase.createTimeSlot(command));
            verify(timeSlotRepository, never()).save(any());
        }
    }
}
