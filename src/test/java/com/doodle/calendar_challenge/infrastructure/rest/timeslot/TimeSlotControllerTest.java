package com.doodle.calendar_challenge.infrastructure.rest.timeslot;

import com.doodle.calendar_challenge.TestcontainersConfiguration;
import com.doodle.calendar_challenge.domain.timeslot.entity.TimeSlot;
import com.doodle.calendar_challenge.domain.timeslot.port.TimeSlotRepository;
import com.doodle.calendar_challenge.domain.timeslot.vo.TimeRange;
import com.doodle.calendar_challenge.infrastructure.persistence.meeting.MeetingJPARepository;
import com.doodle.calendar_challenge.infrastructure.persistence.timeslot.TimeSlotJPARepository;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.CreateTimeSlotRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.TimeSlotSearchRequestDTO;
import com.doodle.calendar_challenge.infrastructure.rest.timeslot.dto.UpdateTimeSlotRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("TimeSlotController")
class TimeSlotControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private TimeSlotRepository timeSlotAdapter;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private TimeSlotJPARepository timeSlotRepository;

    @Autowired
    private MeetingJPARepository meetingRepository;

    private static final String OWNER = "api-alice";
    private static final String OTHER_OWNER = "api-bob";
    private static final Instant BASE = Instant.parse("2030-06-01T09:00:00Z");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        meetingRepository.deleteAll();
        timeSlotRepository.deleteAll();
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/timeslots
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/timeslots")
    class CreateTimeSlot {

        @Test
        @DisplayName("returns 201 with the created slot")
        void returns201WhenValid() throws Exception {
            final var request = new CreateTimeSlotRequestDTO(OWNER, BASE, BASE.plusSeconds(3600), false);

            mockMvc.perform(post("/api/v1/timeslots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.owner").value(OWNER))
                    .andExpect(jsonPath("$.busy").value(false));
        }

        @Test
        @DisplayName("returns 400 when owner is blank")
        void returns400WhenOwnerBlank() throws Exception {
            final var request = new CreateTimeSlotRequestDTO("", BASE, BASE.plusSeconds(3600), false);

            mockMvc.perform(post("/api/v1/timeslots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.owner").exists());
        }

        @Test
        @DisplayName("returns 400 when startAt is null")
        void returns400WhenStartAtNull() throws Exception {
            final var body = """
                    {"owner":"alice","endAt":"2030-06-01T10:00:00Z","busy":false}
                    """;

            mockMvc.perform(post("/api/v1/timeslots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.startAt").exists());
        }

        @Test
        @DisplayName("returns 400 when endAt is before startAt")
        void returns400WhenEndBeforeStart() throws Exception {
            final var request = new CreateTimeSlotRequestDTO(OWNER, BASE, BASE.minusSeconds(1), false);

            mockMvc.perform(post("/api/v1/timeslots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("returns 409 when slot overlaps with an existing one")
        void returns409WhenOverlapping() throws Exception {
            timeSlotAdapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600)));

            final var request = new CreateTimeSlotRequestDTO(OWNER, BASE.plusSeconds(1800), BASE.plusSeconds(5400), false);

            mockMvc.perform(post("/api/v1/timeslots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // -----------------------------------------------------------------------
    // DELETE /api/v1/timeslots/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/timeslots/{id}")
    class DeleteTimeSlot {

        @Test
        @DisplayName("returns 204 when slot exists and is free")
        void returns204WhenFreeSlot() throws Exception {
            final var saved = timeSlotAdapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600)));

            mockMvc.perform(delete("/api/v1/timeslots/{id}", saved.id()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 404 when slot does not exist")
        void returns404WhenNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/timeslots/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when slot has a meeting assigned")
        void returns409WhenSlotHasMeeting() throws Exception {
            final var createSlotRequest = new CreateTimeSlotRequestDTO(OWNER, BASE, BASE.plusSeconds(3600), false);
            final var createSlotResult = mockMvc.perform(post("/api/v1/timeslots")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createSlotRequest)))
                    .andExpect(status().isCreated())
                    .andReturn();

            final var slotId = extractId(createSlotResult);

            final var createMeetingBody = """
                    {"timeSlotId":"%s","title":"Stand-up"}
                    """.formatted(slotId);
            mockMvc.perform(post("/api/v1/meetings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createMeetingBody))
                    .andExpect(status().isCreated());

            mockMvc.perform(delete("/api/v1/timeslots/{id}", slotId))
                    .andExpect(status().isConflict());
        }
    }

    // -----------------------------------------------------------------------
    // GET /api/v1/timeslots/schedule
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/timeslots/schedule")
    class GetSchedule {

        @Test
        @DisplayName("returns 200 with ordered schedule entries")
        void returns200WithEntries() throws Exception {
            timeSlotAdapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600)));
            timeSlotAdapter.save(buildSlot(OWNER, BASE.plusSeconds(7200), BASE.plusSeconds(10800)));

            mockMvc.perform(get("/api/v1/timeslots/schedule").param("owner", OWNER))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].type").value("FREE"));
        }

        @Test
        @DisplayName("returns 200 with empty list when owner has no slots")
        void returns200EmptyListForUnknownOwner() throws Exception {
            mockMvc.perform(get("/api/v1/timeslots/schedule").param("owner", "nobody"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // -----------------------------------------------------------------------
    // PATCH /api/v1/timeslots/{id}
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/timeslots/{id}")
    class UpdateTimeSlot {

        @Test
        @DisplayName("returns 200 with updated slot")
        void returns200WhenValid() throws Exception {
            final var saved = timeSlotAdapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600)));
            final var request = new UpdateTimeSlotRequestDTO(BASE, BASE.plusSeconds(7200), false);

            mockMvc.perform(patch("/api/v1/timeslots/{id}", saved.id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(saved.id().toString()));
        }

        @Test
        @DisplayName("returns 404 when slot does not exist")
        void returns404WhenNotFound() throws Exception {
            final var request = new UpdateTimeSlotRequestDTO(BASE, BASE.plusSeconds(3600), false);

            mockMvc.perform(patch("/api/v1/timeslots/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when update would overlap another slot")
        void returns409WhenOverlapping() throws Exception {
            timeSlotAdapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600)));
            final var slot2 = timeSlotAdapter.save(buildSlot(OWNER, BASE.plusSeconds(7200), BASE.plusSeconds(10800)));
            final var request = new UpdateTimeSlotRequestDTO(BASE.plusSeconds(1800), BASE.plusSeconds(9000), false);

            mockMvc.perform(patch("/api/v1/timeslots/{id}", slot2.id())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // -----------------------------------------------------------------------
    // POST /api/v1/timeslots/search
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/timeslots/search")
    class SearchTimeSlots {

        @Test
        @DisplayName("returns 200 with paginated results")
        void returns200WithResults() throws Exception {
            timeSlotAdapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600)));
            timeSlotAdapter.save(buildSlot(OTHER_OWNER, BASE, BASE.plusSeconds(3600)));

            final var request = new TimeSlotSearchRequestDTO(
                    List.of(OWNER, OTHER_OWNER),
                    BASE.minusSeconds(1), BASE.plusSeconds(3601),
                    null, 0, 10);

            mockMvc.perform(post("/api/v1/timeslots/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.page").value(0));
        }

        @Test
        @DisplayName("filters by busy status when provided")
        void filtersByBusy() throws Exception {
            timeSlotAdapter.save(buildSlot(OWNER, BASE, BASE.plusSeconds(3600)));
            timeSlotAdapter.save(buildSlot(OTHER_OWNER, BASE, BASE.plusSeconds(3600)));
            timeSlotAdapter.markSlotsAsBusy(
                    List.of(timeSlotRepository.findByOwnerOrderByStartAtAsc(OTHER_OWNER).get(0).getId()),
                    UUID.randomUUID());

            final var request = new TimeSlotSearchRequestDTO(
                    List.of(OWNER, OTHER_OWNER),
                    BASE.minusSeconds(1), BASE.plusSeconds(3601),
                    true, 0, 10);

            mockMvc.perform(post("/api/v1/timeslots/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].busy").value(true));
        }

        @Test
        @DisplayName("returns 400 when owners list is empty")
        void returns400WhenOwnersEmpty() throws Exception {
            final var request = new TimeSlotSearchRequestDTO(
                    List.of(),
                    BASE, BASE.plusSeconds(3600),
                    null, 0, 10);

            mockMvc.perform(post("/api/v1/timeslots/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.owners").exists());
        }

        @Test
        @DisplayName("returns 400 when owners list exceeds 50")
        void returns400WhenOwnersTooMany() throws Exception {
            final var owners = IntStream.range(0, 51)
                    .mapToObj(i -> "owner-" + i)
                    .toList();
            final var request = new TimeSlotSearchRequestDTO(
                    owners,
                    BASE, BASE.plusSeconds(3600),
                    null, 0, 10);

            mockMvc.perform(post("/api/v1/timeslots/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.owners").exists());
        }
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private TimeSlot buildSlot(String owner, Instant start, Instant end) {
        return new TimeSlot(
                Generators.timeBasedEpochGenerator().generate(),
                owner,
                new TimeRange(start, end),
                false, null, null);
    }

    private UUID extractId(MvcResult result) throws Exception {
        final var json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }
}
