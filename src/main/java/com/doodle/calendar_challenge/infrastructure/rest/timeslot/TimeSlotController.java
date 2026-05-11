package com.doodle.calendar_challenge.infrastructure.rest.timeslot;

import com.doodle.calendar_challenge.application.timeslot.CreateTimeSlotUseCase;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.CreateTimeSlotRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.TimeSlotResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/timeslots")
@RequiredArgsConstructor
@Slf4j
public class TimeSlotController {

    private final CreateTimeSlotUseCase createTimeSlotUseCase;

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
}
