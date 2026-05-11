package com.doodle.calendar_challenge.application.calendar;

import com.doodle.calendar_challenge.domain.calendar.vo.CalendarEntryType;
import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import com.doodle.calendar_challenge.domain.meeting.port.MeetingRepository;
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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetCalendarByOwnerUseCase")
class GetCalendarByOwnerUseCaseTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @InjectMocks
    private GetCalendarByOwnerUseCase useCase;

    private static final String OWNER     = "rafa";
    private static final UUID   SLOT_ID   = UUID.randomUUID();
    private static final UUID   MEETING_ID = UUID.randomUUID();
    private static final TimeRange TIME_RANGE = new TimeRange(
            Instant.parse("2025-06-01T10:00:00Z"),
            Instant.parse("2025-06-01T11:00:00Z")
    );

    @Test
    @DisplayName("returns FREE entry for a free slot")
    void returnsFreeEntry() {
        final var freeSlot = new TimeSlot(SLOT_ID, OWNER, TIME_RANGE, false, null, null);
        when(timeSlotRepository.findByOwnerOrderByStartAt(OWNER)).thenReturn(List.of(freeSlot));
        when(meetingRepository.findAllById(anySet())).thenReturn(List.of());

        final var result = useCase.getCalendar(OWNER);

        assertEquals(1, result.size());
        assertEquals(CalendarEntryType.FREE, result.get(0).type());
        assertNull(result.get(0).meetingId());
        assertNull(result.get(0).meetingTitle());
    }

    @Test
    @DisplayName("returns BUSY entry for a busy slot without meeting")
    void returnsBusyEntry() {
        final var busySlot = new TimeSlot(SLOT_ID, OWNER, TIME_RANGE, true, null, null);
        when(timeSlotRepository.findByOwnerOrderByStartAt(OWNER)).thenReturn(List.of(busySlot));
        when(meetingRepository.findAllById(anySet())).thenReturn(List.of());

        final var result = useCase.getCalendar(OWNER);

        assertEquals(1, result.size());
        assertEquals(CalendarEntryType.BUSY, result.get(0).type());
        assertNull(result.get(0).meetingId());
    }

    @Test
    @DisplayName("returns MEETING entry with title for a slot linked to a meeting")
    void returnsMeetingEntryWithTitle() {
        final var meetingSlot = new TimeSlot(SLOT_ID, OWNER, TIME_RANGE, true, MEETING_ID, null);
        final var meeting = new Meeting(MEETING_ID, "Team sync", null, Set.of(OWNER), null);

        when(timeSlotRepository.findByOwnerOrderByStartAt(OWNER)).thenReturn(List.of(meetingSlot));
        when(meetingRepository.findAllById(Set.of(MEETING_ID))).thenReturn(List.of(meeting));

        final var result = useCase.getCalendar(OWNER);

        assertEquals(1, result.size());
        assertEquals(CalendarEntryType.MEETING, result.get(0).type());
        assertEquals(MEETING_ID, result.get(0).meetingId());
        assertEquals("Team sync", result.get(0).meetingTitle());
    }

    @Test
    @DisplayName("returns MEETING entry with null title when meeting is not found")
    void returnsMeetingEntryWithNullTitleWhenMeetingMissing() {
        final var meetingSlot = new TimeSlot(SLOT_ID, OWNER, TIME_RANGE, true, MEETING_ID, null);

        when(timeSlotRepository.findByOwnerOrderByStartAt(OWNER)).thenReturn(List.of(meetingSlot));
        when(meetingRepository.findAllById(anySet())).thenReturn(List.of());

        final var result = useCase.getCalendar(OWNER);

        assertEquals(CalendarEntryType.MEETING, result.get(0).type());
        assertNull(result.get(0).meetingTitle());
    }

    @Test
    @DisplayName("returns empty list when owner has no slots")
    void returnsEmptyListWhenNoSlots() {
        when(timeSlotRepository.findByOwnerOrderByStartAt(OWNER)).thenReturn(List.of());
        when(meetingRepository.findAllById(anySet())).thenReturn(List.of());

        assertTrue(useCase.getCalendar(OWNER).isEmpty());
    }
}
