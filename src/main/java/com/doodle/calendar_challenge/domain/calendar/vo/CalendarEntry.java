package com.doodle.calendar_challenge.domain.calendar.vo;

import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;

import java.util.UUID;

public record CalendarEntry(UUID slotId, TimeRange timeRange, CalendarEntryType type, UUID meetingId, String meetingTitle) {
}
