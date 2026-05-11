package com.doodle.calendar_challenge.domain.meeting.vo;

import java.util.Set;
import java.util.UUID;

public record CreateMeetingCommand(UUID timeSlotId, String title, String description, Set<String> participants) {
}
