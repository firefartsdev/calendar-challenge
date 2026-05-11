package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.application.exception.OverlappingTimeSlotException;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.CreateTimeSlotCommand;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CreateTimeSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public TimeSlot createTimeSlot(CreateTimeSlotCommand command) {
        log.info("Creating TimeSlot for owner={}, startAt={}, endAt={}",
                command.owner(), command.startAt(), command.endAt());

        final var timeRange = new TimeRange(command.startAt(), command.endAt());
        final var overlapExists = this.timeSlotRepository.existsOverlappingSlot(command.owner(), timeRange);

        if(overlapExists) {
            log.warn("Overlapping slot detected for owner={}, startAt={}, endAt={}",
                    command.owner(), command.startAt(), command.endAt());
            throw new OverlappingTimeSlotException("TimeSlot is overlapping with an existing timeSlot");
        }

        final var timeSlot = new TimeSlot(UUID.randomUUID(), command.owner(), timeRange, false, null, null);
        return this.timeSlotRepository.save(timeSlot);
    }
}
