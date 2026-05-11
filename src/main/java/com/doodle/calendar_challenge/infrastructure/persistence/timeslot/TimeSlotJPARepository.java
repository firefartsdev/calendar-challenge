package com.doodle.calendar_challenge.infrastructure.persistence.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TimeSlotJPARepository extends JpaRepository<TimeSlotJPAEntity, UUID> {

    @Query("""
        SELECT COUNT(ts) > 0
        FROM TimeSlotJPAEntity ts
        WHERE ts.owner = :owner
            AND ts.startAt < :endAt
            AND ts.endAt > :startAt
    """)
    boolean existsOverlappingSlot(String owner, Instant startAt, Instant endAt);

    List<TimeSlotJPAEntity> findByOwnerOrderByStartAtAsc(String owner);
}
