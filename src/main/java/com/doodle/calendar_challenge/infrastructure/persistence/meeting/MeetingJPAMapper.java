package com.doodle.calendar_challenge.infrastructure.persistence.meeting;

import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MeetingJPAMapper {

    MeetingJPAEntity toEntity(Meeting domain);

    Meeting toDomain(MeetingJPAEntity entity);
}
