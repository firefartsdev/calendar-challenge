package com.doodle.calendar_challenge.infrastructure.persistence.timeslot;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TimeSlotJPARepositoryAdapter implements TimeSlotRepository {

    private final TimeSlotJPARepository timeSlotRepository;

    private final TimeSlotJPAMapper timeSlotMapper;

    @Override
    public TimeSlot save(TimeSlot timeSlot) {
        log.info("Saving TimeSlot {}", timeSlot);
        final var entity  = this.timeSlotMapper.toEntity(timeSlot);
        final var savedEntity = this.timeSlotRepository.save(entity);
        log.info("Saved TimeSlot {}", savedEntity);
        return this.timeSlotMapper.toDomain(savedEntity);
    }

    @Override
    public boolean existsOverlappingSlot(String owner, TimeRange timeRange) {
        log.debug("Checking overlapping slots for owner={}, startAt={}, endAt={}",
                owner, timeRange.startAt(), timeRange.endAt());
        return this.timeSlotRepository.existsOverlappingSlot(owner, timeRange.startAt(), timeRange.endAt());
    }

    @Override
    public List<TimeSlot> findByOwnerOrderByStartAt(String owner) {
        log.debug("Finding TimeSlots for owner={} ordered by startAt", owner);
        return this.timeSlotRepository.findByOwnerOrderByStartAtAsc(owner)
                .stream()
                .map(this.timeSlotMapper::toDomain)
                .toList();
    }
}
