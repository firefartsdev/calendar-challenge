package com.doodle.calendar_challenge.infrastructure.rest.timeslot;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.vo.CreateTimeSlotCommand;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.CreateTimeSlotRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.TimeSlotResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TimeSlotApiMapper {

    CreateTimeSlotCommand toCommand(CreateTimeSlotRequestDTO dto);

    @Mapping(target = "startAt", source = "timeRange.startAt")
    @Mapping(target = "endAt", source = "timeRange.endAt")
    TimeSlotResponseDTO toResponse(TimeSlot timeSlot);
}
