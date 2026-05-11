package com.doodle.calendar_challenge.infrastructure.persistence.timeslot;

import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TimeSlotJPAMapper {

    @Mapping(target = "startAt",  source = "timeRange.startAt")
    @Mapping(target = "endAt",  source = "timeRange.endAt")
    TimeSlotJPAEntity toEntity(TimeSlot domain);

    @Mapping(target = "timeRange", expression = "java(new TimeRange(entity.getStartAt(), entity.getEndAt()))")
    TimeSlot toDomain(TimeSlotJPAEntity entity);

}
