package com.doodle.calendar_challenge.infrastructure.persistence.timeslot;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            AND ts.busy = false
            AND ts.startAt <= :startAt
            AND ts.endAt >= :endAt
        ORDER BY ts.startAt ASC
    """)
    List<TimeSlotJPAEntity> findFreeSlotsCoveringForOwners(Set<String> owners, Instant startAt, Instant endAt);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE TimeSlotJPAEntity ts
        SET ts.busy = true, ts.meetingId = :meetingId, ts.version = ts.version + 1
        WHERE ts.id IN :slotIds
            AND ts.busy = false
    """)
    int markSlotsAsBusy(Collection<UUID> slotIds, UUID meetingId);

    @Query(value = """
        SELECT ts FROM TimeSlotJPAEntity ts
        WHERE ts.owner IN :owners
            AND ts.startAt < :endAt
            AND ts.endAt > :startAt
        """,
        countQuery = """
        SELECT COUNT(ts) FROM TimeSlotJPAEntity ts
        WHERE ts.owner IN :owners
            AND ts.startAt < :endAt
            AND ts.endAt > :startAt
        """)
    Page<TimeSlotJPAEntity> findByOwnersAndTimeRange(List<String> owners, Instant startAt, Instant endAt, Pageable pageable);

    @Query(value = """
        SELECT ts FROM TimeSlotJPAEntity ts
        WHERE ts.owner IN :owners
            AND ts.startAt < :endAt
            AND ts.endAt > :startAt
            AND ts.busy = :busy
        """,
        countQuery = """
        SELECT COUNT(ts) FROM TimeSlotJPAEntity ts
        WHERE ts.owner IN :owners
            AND ts.startAt < :endAt
            AND ts.endAt > :startAt
            AND ts.busy = :busy
        """)
    Page<TimeSlotJPAEntity> findByOwnersAndTimeRangeAndBusy(List<String> owners, Instant startAt, Instant endAt, boolean busy, Pageable pageable);
}
