package com.doodle.calendar_challenge.infrastructure.rest.meeting;

import com.doodle.calendar_challenge.application.meeting.CreateMeetingUseCase;
import com.doodle.calendar_challenge.infrastructure.rest.meeting.dto.CreateMeetingRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.meeting.dto.MeetingResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/meetings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Meetings", description = "Operations for creating and managing meetings")
public class MeetingController {

    private final CreateMeetingUseCase createMeetingUseCase;
    private final MeetingApiMapper meetingApiMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creates a meeting", description = "Creates a meeting linked to a free time slot. The slot must not be busy.")
    @ApiResponse(responseCode = "201", description = "Meeting created successfully.")
    @ApiResponse(responseCode = "404", description = "Time slot not found.")
    @ApiResponse(responseCode = "409", description = "Time slot is not free.")
    public MeetingResponseDTO createMeeting(@Valid @RequestBody CreateMeetingRequestDTO createMeetingRequestDTO) {
        log.info("Meeting create request for timeSlotId={}", createMeetingRequestDTO.timeSlotId());
        final var command = this.meetingApiMapper.toCommand(createMeetingRequestDTO);
        final var meeting = this.createMeetingUseCase.createMeeting(command);
        return this.meetingApiMapper.toResponse(meeting);
    }
}
