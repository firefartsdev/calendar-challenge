package com.doodle.calendar_challenge.application.meeting;

import com.doodle.calendar_challenge.application.exception.ParticipantNotAvailableException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFreeException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFoundException;
import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import com.doodle.calendar_challenge.domain.meeting.port.MeetingRepository;
import com.doodle.calendar_challenge.domain.meeting.vo.CreateMeetingCommand;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CreateMeetingUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final MeetingRepository meetingRepository;
    private final MeterRegistry meterRegistry;

    public Meeting createMeeting(CreateMeetingCommand command) {
        log.info("Creating Meeting for timeSlotId={}", command.timeSlotId());

        final var ownerSlot = timeSlotRepository.findById(command.timeSlotId())
                .orElseThrow(() -> new TimeSlotNotFoundException(command.timeSlotId()));

        if (ownerSlot.busy()) {
            log.warn("TimeSlot id={} is not free", command.timeSlotId());
            meterRegistry.counter("meetings.creation.failed", "reason", "slot_not_free").increment();
            throw new TimeSlotNotFreeException(command.timeSlotId());
        }

        final var participants = command.participants() == null ? new HashSet<String>() : new HashSet<>(command.participants());
        participants.remove(ownerSlot.owner());

        // Batch SELECT: one query for all participants instead of one per participant
        final Map<String, TimeSlot> participantSlots;
        if (!participants.isEmpty()) {
            participantSlots = timeSlotRepository.findFreeSlotsCoveringForOwners(participants, ownerSlot.timeRange());
            final var missing = participants.stream()
                    .filter(p -> !participantSlots.containsKey(p))
                    .findFirst();
            if (missing.isPresent()) {
                log.warn("Participant '{}' has no free slot covering timeRange={}", missing.get(), ownerSlot.timeRange());
                meterRegistry.counter("meetings.creation.failed", "reason", "participant_not_available").increment();
                throw new ParticipantNotAvailableException(missing.get());
            }
        } else {
            participantSlots = Map.of();
        }

        participants.add(ownerSlot.owner());
        final var meeting = new Meeting(UUID.randomUUID(), command.title(), command.description(), participants, null);
        final var savedMeeting = meetingRepository.save(meeting);

        // Batch UPDATE: one statement for all slots instead of one save per slot
        final var allSlotIds = new ArrayList<UUID>();
        allSlotIds.add(ownerSlot.id());
        participantSlots.values().stream().map(TimeSlot::id).forEach(allSlotIds::add);

        final int updated = timeSlotRepository.markSlotsAsBusy(allSlotIds, savedMeeting.id());
        if (updated != allSlotIds.size()) {
            log.warn("Expected to mark {} slot(s) as busy but updated {} — concurrent modification detected",
                    allSlotIds.size(), updated);
            meterRegistry.counter("meetings.creation.failed", "reason", "slot_not_free").increment();
            throw new TimeSlotNotFreeException(ownerSlot.id());
        }

        meterRegistry.counter("meetings.created").increment();
        log.info("Meeting id={} created and linked to {} time slot(s)", savedMeeting.id(), allSlotIds.size());
        return savedMeeting;
    }
}
