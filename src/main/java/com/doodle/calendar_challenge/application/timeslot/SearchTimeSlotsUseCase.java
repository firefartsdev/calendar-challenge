package com.doodle.calendar_challenge.application.timeslot;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.SearchTimeSlotsQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class SearchTimeSlotsUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public List<TimeSlot> searchTimeSlots(SearchTimeSlotsQuery query) {
        log.info("Searching TimeSlots for owners={}, startAt={}, endAt={}, busy={}",
                query.owners(), query.timeRange().startAt(), query.timeRange().endAt(), query.busy());
        return timeSlotRepository.searchByOwnersAndTimeRange(query.owners(), query.timeRange(), query.busy());
    }
}
