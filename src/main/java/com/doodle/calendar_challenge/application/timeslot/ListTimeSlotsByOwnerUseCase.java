package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ListTimeSlotsByOwnerUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public List<TimeSlot> listTimeSlotsByOwner(String owner) {
        log.info("Listing TimeSlots for owner={}", owner);
        return this.timeSlotRepository.findByOwnerOrderByStartAt(owner);
    }
}
