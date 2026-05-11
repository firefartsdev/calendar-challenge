package com.doodle.calendar_challenge.application.calendar;

import com.doodle.calendar_challenge.domain.calendar.vo.CalendarEntry;
import com.doodle.calendar_challenge.domain.calendar.vo.CalendarEntryType;
import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import com.doodle.calendar_challenge.domain.meeting.port.MeetingRepository;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class GetCalendarByOwnerUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final MeetingRepository meetingRepository;

    public List<CalendarEntry> getCalendar(String owner) {
        log.info("Getting calendar for owner={}", owner);

        final var slots = this.timeSlotRepository.findByOwnerOrderByStartAt(owner);

        final var meetingIds = slots.stream()
                .map(TimeSlot::meetingId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<UUID, Meeting> meetingsById = this.meetingRepository.findAllById(meetingIds)
                .stream()
                .collect(Collectors.toMap(Meeting::id, m -> m));

        return slots.stream()
                .map(slot -> toCalendarEntry(slot, meetingsById))
                .toList();
    }

    private CalendarEntry toCalendarEntry(TimeSlot slot, Map<UUID, Meeting> meetingsById) {
        if (slot.meetingId() != null) {
            final var meeting = meetingsById.get(slot.meetingId());
            return new CalendarEntry(slot.id(), slot.timeRange(), CalendarEntryType.MEETING,
                    slot.meetingId(), meeting != null ? meeting.title() : null);
        }
        final var type = slot.busy() ? CalendarEntryType.BUSY : CalendarEntryType.FREE;
        return new CalendarEntry(slot.id(), slot.timeRange(), type, null, null);
    }
}
