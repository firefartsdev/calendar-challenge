package com.doodle.calendar_challenge.application.meeting;

import com.doodle.calendar_challenge.application.exception.ParticipantNotAvailableException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFreeException;
import com.doodle.calendar_challenge.application.exception.TimeSlotNotFoundException;
import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import com.doodle.calendar_challenge.domain.meeting.port.MeetingRepository;
import com.doodle.calendar_challenge.domain.meeting.vo.CreateMeetingCommand;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CreateMeetingUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final MeetingRepository meetingRepository;

    public Meeting createMeeting(CreateMeetingCommand command) {
        log.info("Creating Meeting for timeSlotId={}", command.timeSlotId());

        final var ownerSlot = this.timeSlotRepository.findById(command.timeSlotId())
                .orElseThrow(() -> new TimeSlotNotFoundException(command.timeSlotId()));

        if (ownerSlot.busy()) {
            log.warn("TimeSlot id={} is not free", command.timeSlotId());
            throw new TimeSlotNotFreeException(command.timeSlotId());
        }

        // Collect a free covering slot for every requested participant (owner excluded, checked separately).
        // Fail fast if any participant has no available slot.
        final var participants = command.participants() == null ? new HashSet<String>() : new HashSet<>(command.participants());
        participants.remove(ownerSlot.owner());

        final var participantSlots = participants.stream()
                .collect(Collectors.toMap(
                        p -> p,
                        p -> this.timeSlotRepository.findFreeSlotCovering(p, ownerSlot.timeRange())
                                .orElseThrow(() -> {
                                    log.warn("Participant '{}' has no free slot covering timeRange={}", p, ownerSlot.timeRange());
                                    return new ParticipantNotAvailableException(p);
                                })
                ));

        participants.add(ownerSlot.owner());
        final var meeting = new Meeting(UUID.randomUUID(), command.title(), command.description(),
                participants, null);
        final var savedMeeting = this.meetingRepository.save(meeting);

        // Mark the owner's slot and every participant's slot as busy.
        markSlotBusy(ownerSlot, savedMeeting.id());
        participantSlots.values().forEach(slot -> markSlotBusy(slot, savedMeeting.id()));

        log.info("Meeting id={} created and linked to {} time slot(s)", savedMeeting.id(), 1 + participantSlots.size());
        return savedMeeting;
    }

    private void markSlotBusy(TimeSlot slot, UUID meetingId) {
        final var busySlot = new TimeSlot(slot.id(), slot.owner(), slot.timeRange(), true, meetingId, slot.version());
        this.timeSlotRepository.save(busySlot);
    }
}
