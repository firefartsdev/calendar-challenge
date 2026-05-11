package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.application.exception.TimeSlotHasMeetingException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFoundException;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class DeleteTimeSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public void deleteTimeSlot(UUID id) {
        log.info("Deleting TimeSlot id={}", id);

        final var timeSlot = this.timeSlotRepository.findById(id)
                .orElseThrow(() -> new TimeSlotNotFoundException(id));

        if (timeSlot.meetingId() != null) {
            log.warn("TimeSlot id={} has meeting id={} assigned, cannot delete", id, timeSlot.meetingId());
            throw new TimeSlotHasMeetingException(id);
        }

        this.timeSlotRepository.deleteById(id);
    }
}
