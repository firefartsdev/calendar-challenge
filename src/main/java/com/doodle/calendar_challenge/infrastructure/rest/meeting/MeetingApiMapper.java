package com.doodle.calendar_challenge.infrastructure.rest.meeting;

import com.doodle.calendar_challenge.domain.meeting.entity.Meeting;
import com.doodle.calendar_challenge.domain.meeting.vo.CreateMeetingCommand;
import com.doodle.calendar_challenge.infrastructure.rest.meeting.dto.CreateMeetingRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.meeting.dto.MeetingResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MeetingApiMapper {

    CreateMeetingCommand toCommand(CreateMeetingRequestDTO dto);

    MeetingResponseDTO toResponse(Meeting meeting);
}
