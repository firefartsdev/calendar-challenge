package com.doodle.calendar_challenge.infrastructure.rest.meeting;

import com.doodle.calendar_challenge.TestcontainersConfiguration;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import com.doodle.calendar_challenge.infrastructure.persistence.meeting.MeetingJPARepository;
import com.doodle.calendar_challenge.infrastructure.persistence.timeslot.TimeSlotJPARepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.uuid.Generators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("MeetingController")
class MeetingControllerTest {

    @Autowired
    private WebApplicationContext wac;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private TimeSlotRepository timeSlotAdapter;

    private MockMvc mockMvc;

    @Autowired
    private TimeSlotJPARepository timeSlotRepository;

    @Autowired
    private MeetingJPARepository meetingRepository;

    private static final String OWNER = "meeting-alice";
    private static final String PARTICIPANT = "meeting-bob";
    private static final Instant BASE = Instant.parse("2030-07-01T10:00:00Z");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        meetingRepository.deleteAll();
        timeSlotRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/v1/meetings")
    class CreateMeeting {

        @Test
        @DisplayName("returns 201 with created meeting")
        void returns201WhenValid() throws Exception {
            final var slot = timeSlotAdapter.save(buildFreeSlot(OWNER));

            final var body = objectMapper.writeValueAsString(
                    new CreateMeetingBody(slot.id(), "Team Sync", null, null));

            mockMvc.perform(post("/api/v1/meetings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.title").value("Team Sync"));
        }

        @Test
        @DisplayName("returns 201 with participants in response")
        void returns201WithParticipants() throws Exception {
            final var ownerSlot = timeSlotAdapter.save(buildFreeSlot(OWNER));
            final var participantSlot = timeSlotAdapter.save(buildFreeSlot(PARTICIPANT));

            final var body = objectMapper.writeValueAsString(
                    new CreateMeetingBody(ownerSlot.id(), "Design Review", "Q2 review", Set.of(PARTICIPANT)));

            mockMvc.perform(post("/api/v1/meetings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.participants").isArray())
                    .andExpect(jsonPath("$.participants", org.hamcrest.Matchers.hasItem(PARTICIPANT)));

            // both slots should now be busy
            final var ownerEntity = timeSlotRepository.findById(ownerSlot.id()).orElseThrow();
            final var participantEntity = timeSlotRepository.findById(participantSlot.id()).orElseThrow();
            org.junit.jupiter.api.Assertions.assertTrue(ownerEntity.isBusy());
            org.junit.jupiter.api.Assertions.assertTrue(participantEntity.isBusy());
        }

        @Test
        @DisplayName("returns 400 when title is missing")
        void returns400WhenTitleMissing() throws Exception {
            final var slot = timeSlotAdapter.save(buildFreeSlot(OWNER));

            final var body = """
                    {"timeSlotId":"%s"}
                    """.formatted(slot.id());

            mockMvc.perform(post("/api/v1/meetings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.title").exists());
        }

        @Test
        @DisplayName("returns 404 when timeslot does not exist")
        void returns404WhenTimeSlotNotFound() throws Exception {
            final var body = objectMapper.writeValueAsString(
                    new CreateMeetingBody(UUID.randomUUID(), "Stand-up", null, null));

            mockMvc.perform(post("/api/v1/meetings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when timeslot is already busy")
        void returns409WhenTimeSlotBusy() throws Exception {
            final var slot = timeSlotAdapter.save(buildFreeSlot(OWNER));
            timeSlotAdapter.markSlotsAsBusy(java.util.List.of(slot.id()), UUID.randomUUID());

            final var body = objectMapper.writeValueAsString(
                    new CreateMeetingBody(slot.id(), "Stand-up", null, null));

            mockMvc.perform(post("/api/v1/meetings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 409 when a participant has no free covering slot")
        void returns409WhenParticipantNotAvailable() throws Exception {
            final var ownerSlot = timeSlotAdapter.save(buildFreeSlot(OWNER));
            // participant has NO slot

            final var body = objectMapper.writeValueAsString(
                    new CreateMeetingBody(ownerSlot.id(), "Stand-up", null, Set.of("no-slot-user")));

            mockMvc.perform(post("/api/v1/meetings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private TimeSlot buildFreeSlot(String owner) {
        return new TimeSlot(
                Generators.timeBasedEpochGenerator().generate(),
                owner,
                new TimeRange(BASE, BASE.plusSeconds(3600)),
                false, null, null);
    }

    private record CreateMeetingBody(UUID timeSlotId, String title, String description, Set<String> participants) {}
}
