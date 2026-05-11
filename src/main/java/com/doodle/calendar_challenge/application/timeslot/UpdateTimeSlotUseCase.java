package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.application.exception.OverlappingTimeSlotException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFoundException;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import com.doodle.calendar_challenge.domain.timeslot.vo.UpdateTimeSlotCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UpdateTimeSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public TimeSlot updateTimeSlot(UpdateTimeSlotCommand command) {
        log.info("Updating TimeSlot {}", command);

        final var existing = this.timeSlotRepository.findById(command.id())
                .orElseThrow(() -> new TimeSlotNotFoundException(command.id()));

        final var timeRange = new TimeRange(command.startAt(), command.endAt());
        final var overlapExists = this.timeSlotRepository.existsOverlappingSlotExcluding(
                existing.owner(), timeRange, command.id());

        if (overlapExists) {
            log.warn("Overlapping slot detected for owner={}, startAt={}, endAt={}",
                    existing.owner(), command.startAt(), command.endAt());
            throw new OverlappingTimeSlotException("TimeSlot is overlapping with an existing timeSlot");
        }

        final var updated = new TimeSlot(existing.id(), existing.owner(), timeRange, command.busy(), existing.meetingId(), existing.version());
        return this.timeSlotRepository.save(updated);
    }
}
