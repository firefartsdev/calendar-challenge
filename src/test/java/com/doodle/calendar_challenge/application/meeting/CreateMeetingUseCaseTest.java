package com.doodle.calendar_challenge.application.meeting;

import com.doodle.calendar_challenge.application.exception.ParticipantNotAvailableException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFreeException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFoundException;
import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import com.doodle.calendar_challenge.domain.meeting.port.MeetingRepository;
import com.doodle.calendar_challenge.domain.meeting.vo.CreateMeetingCommand;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateMeetingUseCase")
class CreateMeetingUseCaseTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private MeetingRepository meetingRepository;

    private CreateMeetingUseCase useCase;

    private static final UUID   SLOT_ID  = UUID.randomUUID();
    private static final String OWNER    = "rafa";
    private static final String PARTICIPANT = "isaac";
    private static final TimeRange TIME_RANGE = new TimeRange(
            Instant.parse("2025-06-01T10:00:00Z"),
            Instant.parse("2025-06-01T11:00:00Z")
    );
    private static final TimeSlot FREE_OWNER_SLOT = new TimeSlot(SLOT_ID, OWNER, TIME_RANGE, false, null, 1L);

    @BeforeEach
    void setUp() {
        useCase = new CreateMeetingUseCase(timeSlotRepository, meetingRepository, new SimpleMeterRegistry());
    }

    @Nested
    @DisplayName("successful creation")
    class SuccessfulCreation {

        @Test
        @DisplayName("creates meeting and marks owner slot as busy")
        void createsMeetingAndMarksOwnerSlotBusy() {
            final var command = new CreateMeetingCommand(SLOT_ID, "Team sync", null, Set.of());
            final var savedMeeting = new Meeting(UUID.randomUUID(), "Team sync", null, Set.of(OWNER), null);

            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(FREE_OWNER_SLOT));
            when(meetingRepository.save(any())).thenReturn(savedMeeting);

            useCase.createMeeting(command);

            final var slotCaptor = ArgumentCaptor.forClass(TimeSlot.class);
            verify(timeSlotRepository, atLeastOnce()).save(slotCaptor.capture());

            final var savedSlot = slotCaptor.getAllValues().stream()
                    .filter(s -> s.id().equals(SLOT_ID))
                    .findFirst().orElseThrow();
            assertTrue(savedSlot.busy());
            assertEquals(savedMeeting.id(), savedSlot.meetingId());
        }

        @Test
        @DisplayName("owner is added to participants automatically")
        void ownerAddedToParticipantsAutomatically() {
            final var command = new CreateMeetingCommand(SLOT_ID, "Team sync", null, Set.of(PARTICIPANT));
            final var participantSlot = new TimeSlot(UUID.randomUUID(), PARTICIPANT, TIME_RANGE, false, null, 1L);

            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(FREE_OWNER_SLOT));
            when(timeSlotRepository.findFreeSlotCovering(eq(PARTICIPANT), eq(TIME_RANGE)))
                    .thenReturn(Optional.of(participantSlot));

            final var meetingCaptor = ArgumentCaptor.forClass(Meeting.class);
            when(meetingRepository.save(meetingCaptor.capture())).thenAnswer(i -> i.getArgument(0));

            useCase.createMeeting(command);

            assertTrue(meetingCaptor.getValue().participants().contains(OWNER));
            assertTrue(meetingCaptor.getValue().participants().contains(PARTICIPANT));
        }

        @Test
        @DisplayName("marks all participant slots as busy with the meeting id")
        void marksAllParticipantSlotsBusy() {
            final var participantSlotId = UUID.randomUUID();
            final var participantSlot = new TimeSlot(participantSlotId, PARTICIPANT, TIME_RANGE, false, null, 1L);
            final var command = new CreateMeetingCommand(SLOT_ID, "Team sync", null, Set.of(PARTICIPANT));
            final var savedMeeting = new Meeting(UUID.randomUUID(), "Team sync", null, Set.of(OWNER, PARTICIPANT), null);

            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(FREE_OWNER_SLOT));
            when(timeSlotRepository.findFreeSlotCovering(eq(PARTICIPANT), eq(TIME_RANGE)))
                    .thenReturn(Optional.of(participantSlot));
            when(meetingRepository.save(any())).thenReturn(savedMeeting);

            useCase.createMeeting(command);

            verify(timeSlotRepository, atLeastOnce()).save(argThat(s ->
                    s.id().equals(participantSlotId) && s.busy() && savedMeeting.id().equals(s.meetingId())));
        }
    }

    @Nested
    @DisplayName("failed creation")
    class FailedCreation {

        @Test
        @DisplayName("throws TimeSlotNotFoundException when owner slot does not exist")
        void throwsWhenOwnerSlotNotFound() {
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.empty());
            final var command = new CreateMeetingCommand(SLOT_ID, "Team sync", null, Set.of());

            assertThrows(TimeSlotNotFoundException.class, () -> useCase.createMeeting(command));
            verify(meetingRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws TimeSlotNotFreeException when owner slot is busy")
        void throwsWhenOwnerSlotIsBusy() {
            final var busySlot = new TimeSlot(SLOT_ID, OWNER, TIME_RANGE, true, null, 1L);
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(busySlot));
            final var command = new CreateMeetingCommand(SLOT_ID, "Team sync", null, Set.of());

            assertThrows(TimeSlotNotFreeException.class, () -> useCase.createMeeting(command));
            verify(meetingRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ParticipantNotAvailableException when participant has no free slot")
        void throwsWhenParticipantHasNoFreeSlot() {
            when(timeSlotRepository.findById(SLOT_ID)).thenReturn(Optional.of(FREE_OWNER_SLOT));
            when(timeSlotRepository.findFreeSlotCovering(eq(PARTICIPANT), eq(TIME_RANGE)))
                    .thenReturn(Optional.empty());
            final var command = new CreateMeetingCommand(SLOT_ID, "Team sync", null, Set.of(PARTICIPANT));

            assertThrows(ParticipantNotAvailableException.class, () -> useCase.createMeeting(command));
            verify(meetingRepository, never()).save(any());
        }
    }
}
