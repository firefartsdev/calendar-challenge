package com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto;

import com.doodle.calendar_challenge.domain.calendar.vo.CalendarEntryType;

import java.time.Instant;
import java.util.UUID;

public record TimeSlotScheduleEntryDTO(
        UUID slotId,
        Instant startAt,
        Instant endAt,
        CalendarEntryType type,
        UUID meetingId,
        String meetingTitle
) {
}
