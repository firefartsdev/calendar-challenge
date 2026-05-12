package com.doodle.calendar_challenge.infrastructure.persistence.timeslot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

    @Query("""
        SELECT COUNT(ts) > 0
        FROM TimeSlotJPAEntity ts
        WHERE ts.owner = :owner
            AND ts.startAt < :endAt
            AND ts.endAt > :startAt
            AND ts.id <> :excludeId
    """)
    boolean existsOverlappingSlotExcluding(String owner, Instant startAt, Instant endAt, UUID excludeId);

    Optional<TimeSlotJPAEntity> findFirstByOwnerAndBusyFalseAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
            String owner, Instant startAt, Instant endAt);

    List<TimeSlotJPAEntity> findByOwnerOrderByStartAtAsc(String owner);

    @Query("""
        SELECT ts FROM TimeSlotJPAEntity ts
        WHERE ts.owner IN :owners
            AND ts.startAt < :endAt
            AND ts.endAt > :startAt
        ORDER BY ts.owner ASC, ts.startAt ASC
    """)
    List<TimeSlotJPAEntity> findByOwnersAndTimeRange(List<String> owners, Instant startAt, Instant endAt);

    @Query("""
        SELECT ts FROM TimeSlotJPAEntity ts
        WHERE ts.owner IN :owners
            AND ts.startAt < :endAt
            AND ts.endAt > :startAt
            AND ts.busy = :busy
        ORDER BY ts.owner ASC, ts.startAt ASC
    """)
    List<TimeSlotJPAEntity> findByOwnersAndTimeRangeAndBusy(List<String> owners, Instant startAt, Instant endAt, boolean busy);
}
