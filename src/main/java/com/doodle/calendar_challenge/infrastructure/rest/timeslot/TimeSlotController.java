package com.doodle.calendar_challenge.infrastructure.rest.timeslot;

import com.doodle.calendar_challenge.application.timeslot.CreateTimeSlotUseCase;
import com.doodle.calendar_challenge.application.timeslot.ListTimeSlotsByOwnerUseCase;
import com.doodle.calendar_challenge.application.timeslot.UpdateTimeSlotUseCase;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.CreateTimeSlotRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.TimeSlotResponseDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.UpdateTimeSlotRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timeslots")
@RequiredArgsConstructor
@Slf4j
public class TimeSlotController {

    private final CreateTimeSlotUseCase createTimeSlotUseCase;

    private final ListTimeSlotsByOwnerUseCase listTimeSlotsByOwnerUseCase;

    private final UpdateTimeSlotUseCase updateTimeSlotUseCase;

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
