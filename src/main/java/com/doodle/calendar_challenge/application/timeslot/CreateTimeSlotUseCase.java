package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.application.exception.OverlappingTimeSlotException;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.CreateTimeSlotCommand;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.uuid.Generators;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CreateTimeSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final MeterRegistry meterRegistry;

    public TimeSlot createTimeSlot(CreateTimeSlotCommand command) {
        log.info("Creating TimeSlot {}", command);

        final var timeRange = new TimeRange(command.startAt(), command.endAt());
        final var overlapExists = this.timeSlotRepository.existsOverlappingSlot(command.owner(), timeRange);

        if(overlapExists) {
            log.warn("Overlapping slot detected for owner={}, startAt={}, endAt={}",
                    command.owner(), command.startAt(), command.endAt());
            meterRegistry.counter("timeslots.creation.failed", "reason", "overlap").increment();
            throw new OverlappingTimeSlotException("TimeSlot is overlapping with an existing timeSlot");
        }

        final var timeSlot = new TimeSlot(Generators.timeBasedEpochGenerator().generate(), command.owner(), timeRange, command.busy(), null, null);
        final var saved = this.timeSlotRepository.save(timeSlot);
        meterRegistry.counter("timeslots.created").increment();
        return saved;
    }
}
