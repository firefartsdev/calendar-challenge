package com.doodle.calendar_challenge.infrastructure.persistence.timeslot;

import com.doodle.calendar_challenge.domain.shared.PagedResult;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<TimeSlot> findById(UUID id) {
        log.debug("Finding TimeSlot by id={}", id);
        return this.timeSlotRepository.findById(id)
                .map(this.timeSlotMapper::toDomain);
    }

    @Override
    public boolean existsOverlappingSlot(String owner, TimeRange timeRange) {
        log.debug("Checking overlapping slots for owner={}, startAt={}, endAt={}",
                owner, timeRange.startAt(), timeRange.endAt());
        return this.timeSlotRepository.existsOverlappingSlot(owner, timeRange.startAt(), timeRange.endAt());
    }

    @Override
    public boolean existsOverlappingSlotExcluding(String owner, TimeRange timeRange, UUID excludeId) {
        log.debug("Checking overlapping slots for owner={}, startAt={}, endAt={}, excludeId={}",
                owner, timeRange.startAt(), timeRange.endAt(), excludeId);
        return this.timeSlotRepository.existsOverlappingSlotExcluding(owner, timeRange.startAt(), timeRange.endAt(), excludeId);
    }

    @Override
    public void deleteById(UUID id) {
        log.info("Deleting TimeSlot by id={}", id);
        this.timeSlotRepository.deleteById(id);
    }

    @Override
    public Optional<TimeSlot> findFreeSlotCovering(String owner, TimeRange timeRange) {
        log.debug("Finding free covering slot for owner={}, startAt={}, endAt={}",
                owner, timeRange.startAt(), timeRange.endAt());
        return this.timeSlotRepository.findFirstByOwnerAndBusyFalseAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
                        owner, timeRange.startAt(), timeRange.endAt())
                .map(this.timeSlotMapper::toDomain);
    }

    @Override
    public List<TimeSlot> findByOwnerOrderByStartAt(String owner) {
        log.debug("Finding TimeSlots for owner={} ordered by startAt", owner);
        return this.timeSlotRepository.findByOwnerOrderByStartAtAsc(owner)
                .stream()
                .map(this.timeSlotMapper::toDomain)
                .toList();
    }

    @Override
    public PagedResult<TimeSlot> searchByOwnersAndTimeRange(List<String> owners, TimeRange timeRange, Boolean busy, int page, int size) {
        log.debug("Searching TimeSlots for owners={}, startAt={}, endAt={}, busy={}, page={}, size={}",
                owners, timeRange.startAt(), timeRange.endAt(), busy, page, size);
        final var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("owner"), Sort.Order.asc("startAt")));
        final var result = busy == null
                ? this.timeSlotRepository.findByOwnersAndTimeRange(owners, timeRange.startAt(), timeRange.endAt(), pageable)
                : this.timeSlotRepository.findByOwnersAndTimeRangeAndBusy(owners, timeRange.startAt(), timeRange.endAt(), busy, pageable);
        final var content = result.getContent().stream()
                .map(this.timeSlotMapper::toDomain)
                .toList();
        return new PagedResult<>(content, result.getTotalElements(), result.getTotalPages(), page, size);
    }
}
