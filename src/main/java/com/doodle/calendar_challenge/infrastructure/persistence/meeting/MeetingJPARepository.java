package com.doodle.calendar_challenge.infrastructure.persistence.meeting;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MeetingJPARepository extends JpaRepository<MeetingJPAEntity, UUID> {
}
