package com.doodle.calendar_challenge.infrastructure.persistence.meeting;

import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import com.doodle.calendar_challenge.domain.meeting.port.MeetingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MeetingJPARepositoryAdapter implements MeetingRepository {

    private final MeetingJPARepository meetingRepository;
    private final MeetingJPAMapper meetingMapper;

    @Override
    public Meeting save(Meeting meeting) {
        log.info("Saving Meeting {}", meeting);
        final var entity = this.meetingMapper.toEntity(meeting);
        final var savedEntity = this.meetingRepository.save(entity);
        log.info("Saved Meeting {}", savedEntity);
        return this.meetingMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Meeting> findById(UUID id) {
        log.debug("Finding Meeting by id={}", id);
        return this.meetingRepository.findById(id)
                .map(this.meetingMapper::toDomain);
    }
}
