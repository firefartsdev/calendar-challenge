package com.doodle.calendar_challenge.domain.timeslot.port;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;

import java.util.List;

public interface TimeSlotRepository {

    TimeSlot save(TimeSlot timeSlot);

    boolean existsOverlappingSlot(String owner, TimeRange timeRange);

    List<TimeSlot> findByOwnerOrderByStartAt(String owner);
}
