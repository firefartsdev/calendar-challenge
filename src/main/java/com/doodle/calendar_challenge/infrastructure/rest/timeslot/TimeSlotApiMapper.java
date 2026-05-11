package com.doodle.calendar_challenge.infrastructure.rest.timeslot;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.vo.CreateTimeSlotCommand;
import com.doodle.calendar_challenge.domain.timeslot.vo.UpdateTimeSlotCommand;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.CreateTimeSlotRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.TimeSlotResponseDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.UpdateTimeSlotRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TimeSlotApiMapper {

    CreateTimeSlotCommand toCommand(CreateTimeSlotRequestDTO dto);

    default UpdateTimeSlotCommand toCommand(UUID id, UpdateTimeSlotRequestDTO dto) {
        return new UpdateTimeSlotCommand(id, dto.startAt(), dto.endAt(), dto.busy());
    }

    @Mapping(target = "startAt", source = "timeRange.startAt")
    @Mapping(target = "endAt", source = "timeRange.endAt")
    TimeSlotResponseDTO toResponse(TimeSlot timeSlot);
}
