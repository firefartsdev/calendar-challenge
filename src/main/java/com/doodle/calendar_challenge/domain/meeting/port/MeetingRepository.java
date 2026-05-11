package com.doodle.calendar_challenge.domain.meeting.port;

import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;

import java.util.Optional;

public interface MeetingRepository {

    Meeting save(Meeting meeting);

    Optional<Meeting> findById(String id);
}
