package com.doodle.calendar_challenge.infrastructure.rest.timeslot;

import com.doodle.calendar_challenge.application.calendar.GetCalendarByOwnerUseCase;
import com.doodle.calendar_challenge.application.timeslot.CreateTimeSlotUseCase;
import com.doodle.calendar_challenge.application.timeslot.DeleteTimeSlotUseCase;
import com.doodle.calendar_challenge.application.timeslot.ListTimeSlotsByOwnerUseCase;
import com.doodle.calendar_challenge.application.timeslot.SearchTimeSlotsUseCase;
import com.doodle.calendar_challenge.application.timeslot.UpdateTimeSlotUseCase;
import com.doodle.calendar_challenge.domain.timeslot.vo.SearchTimeSlotsQuery;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.CreateTimeSlotRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.TimeSlotResponseDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.TimeSlotScheduleEntryDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.UpdateTimeSlotRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timeslots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Time Slots", description = "Operations for managing personal time slots and viewing the schedule")
public class TimeSlotController {

    private final CreateTimeSlotUseCase createTimeSlotUseCase;

    private final ListTimeSlotsByOwnerUseCase listTimeSlotsByOwnerUseCase;

    private final UpdateTimeSlotUseCase updateTimeSlotUseCase;

    private final DeleteTimeSlotUseCase deleteTimeSlotUseCase;

    private final GetCalendarByOwnerUseCase getCalendarByOwnerUseCase;

    private final SearchTimeSlotsUseCase searchTimeSlotsUseCase;

    private final TimeSlotApiMapper timeSlotApiMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a time slot", description = "Creates a free time slot for a specific user.")
    @ApiResponse(responseCode = "201", description = "Time slot created successfully.")
    @ApiResponse(responseCode = "409", description = "Overlappìng time slot detected.")
    public TimeSlotResponseDTO createTimeSlot(@Valid @RequestBody CreateTimeSlotRequestDTO createTimeSlotRequestDTO) {
        log.info("Time slot create request for owner={}, startAt={}, endAt={}",
            createTimeSlotRequestDTO.owner(),  createTimeSlotRequestDTO.startAt(), createTimeSlotRequestDTO.endAt());
        final var command = this.timeSlotApiMapper.toCommand(createTimeSlotRequestDTO);
        final var createdTimeSlot = this.createTimeSlotUseCase.createTimeSlot(command);
        return this.timeSlotApiMapper.toResponse(createdTimeSlot);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "List time slots by owner", description = "Returns all time slots for a given owner ordered by start date ascending.")
    @ApiResponse(responseCode = "200", description = "Time slots retrieved successfully.")
    public List<TimeSlotResponseDTO> listTimeSlotsByOwner(@RequestParam String owner) {
        log.info("Time slot list request for owner={}", owner);
        final var timeSlots = this.listTimeSlotsByOwnerUseCase.listTimeSlotsByOwner(owner);
        return timeSlots.stream().map(this.timeSlotApiMapper::toResponse).toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletes a time slot", description = "Deletes a time slot. A time slot with an assigned meeting cannot be deleted.")
    @ApiResponse(responseCode = "204", description = "Time slot deleted successfully.")
    @ApiResponse(responseCode = "404", description = "Time slot not found.")
    @ApiResponse(responseCode = "409", description = "Time slot has a meeting assigned.")
    public void deleteTimeSlot(@PathVariable UUID id) {
        log.info("Time slot delete request for id={}", id);
        this.deleteTimeSlotUseCase.deleteTimeSlot(id);
    }

    @GetMapping("/schedule")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get schedule by owner", description = "Returns all time slots for an owner as schedule entries ordered by start date. Each entry is FREE, BUSY, or MEETING (includes meeting title).")
    @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully.")
    public List<TimeSlotScheduleEntryDTO> getSchedule(@RequestParam String owner) {
        log.info("Schedule request for owner={}", owner);
        return this.getCalendarByOwnerUseCase.getCalendar(owner)
                .stream()
                .map(this.timeSlotApiMapper::toScheduleEntry)
                .toList();
    }

    @GetMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Search time slots by owners and time range",
               description = "Returns time slots for one or more owners that overlap with the given time range. " +
                             "Optionally filter by busy status: true = only busy, false = only free, omit = both.")
    @ApiResponse(responseCode = "200", description = "Time slots retrieved successfully.")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters.")
    public List<TimeSlotResponseDTO> searchTimeSlots(
            @RequestParam List<String> owners,
            @RequestParam Instant startAt,
            @RequestParam Instant endAt,
            @RequestParam(required = false) Boolean busy) {
        log.info("Time slot search request for owners={}, startAt={}, endAt={}, busy={}", owners, startAt, endAt, busy);
        final var query = new SearchTimeSlotsQuery(owners, new TimeRange(startAt, endAt), busy);
        return this.searchTimeSlotsUseCase.searchTimeSlots(query)
                .stream()
                .map(this.timeSlotApiMapper::toResponse)
                .toList();
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Updates a time slot", description = "Updates the time range and busy status of an existing time slot. The owner cannot be changed.")
    @ApiResponse(responseCode = "200", description = "Time slot updated successfully.")
    @ApiResponse(responseCode = "404", description = "Time slot not found.")
    @ApiResponse(responseCode = "409", description = "Overlapping time slot detected.")
    public TimeSlotResponseDTO updateTimeSlot(@PathVariable UUID id, @Valid @RequestBody UpdateTimeSlotRequestDTO updateTimeSlotRequestDTO) {
        log.info("Time slot update request for id={}, startAt={}, endAt={}",
            id, updateTimeSlotRequestDTO.startAt(), updateTimeSlotRequestDTO.endAt());
        final var command = this.timeSlotApiMapper.toCommand(id, updateTimeSlotRequestDTO);
        final var updatedTimeSlot = this.updateTimeSlotUseCase.updateTimeSlot(command);
        return this.timeSlotApiMapper.toResponse(updatedTimeSlot);
    }
}
