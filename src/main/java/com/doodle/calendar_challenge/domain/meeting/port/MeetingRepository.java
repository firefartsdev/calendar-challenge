package com.doodle.calendar_challenge.domain.meeting.port;

import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;

import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository {

    Meeting save(Meeting meeting);

    Optional<Meeting> findById(UUID id);
}
