package com.doodle.calendar_challenge.domain.timeslot.port;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeSlotRepository {

    TimeSlot save(TimeSlot timeSlot);

    Optional<TimeSlot> findById(UUID id);

    boolean existsOverlappingSlot(String owner, TimeRange timeRange);

    boolean existsOverlappingSlotExcluding(String owner, TimeRange timeRange, UUID excludeId);

    Optional<TimeSlot> findFreeSlotCovering(String owner, TimeRange timeRange);

    List<TimeSlot> findByOwnerOrderByStartAt(String owner);
}
