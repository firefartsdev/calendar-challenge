# 📅 Calendar challenge

API to manage calendar with Time Slots and Meetings.

## 🛠️ Setup and execution

### Prerequisites

- Docker and Docker Compose
- Java 21 + Maven (only needed for local development)

---

### Option 1 — Full stack in Docker

Builds and runs the app together with PostgreSQL, Prometheus and Grafana.

```bash
docker compose up --build
```

To stop:

```bash
docker compose down
```

To stop and remove all persisted data (database, metrics, dashboards):

```bash
docker compose down -v
```

---

### Option 2 — Local app + monitoring stack in Docker

Runs only PostgreSQL, Prometheus and Grafana in Docker while the app runs on the host machine.
Useful during development to get fast feedback without rebuilding the Docker image on every change.

```bash
# 1. Start the monitoring stack
docker compose -f docker-compose.yml -f docker-compose.local.yml up

# 2. Start the app locally (in a separate terminal)
./mvnw spring-boot:run
```

To stop the monitoring stack:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

---

### Service URLs

| Service    | URL                                       |
|------------|-------------------------------------------|
| App        | http://localhost:8080                     |
| Swagger UI | http://localhost:8080/swagger-ui.html     |
| Prometheus | http://localhost:9090                     |
| Grafana    | http://localhost:3000 (admin / admin)     |

---

## API

A full interactive reference is available via Swagger UI at http://localhost:8080/swagger-ui.html once the app is running.

| Method   | Path                          | Description                                   |
|----------|-------------------------------|-----------------------------------------------|
| `POST`   | `/api/v1/timeslots`           | Create a time slot                            |
| `PATCH`  | `/api/v1/timeslots/{id}`      | Update a time slot                            |
| `DELETE` | `/api/v1/timeslots/{id}`      | Delete a time slot                            |
| `GET`    | `/api/v1/timeslots/schedule`  | Get the schedule for an owner                 |
| `POST`   | `/api/v1/timeslots/search`    | Search time slots with filters and pagination |
| `POST`   | `/api/v1/meetings`            | Create a meeting                              |

---

## Design decisions

The code is structured following the principles of **Hexagonal Architecture, Domain-Driven Design (DDD)** and **Clean Code**.
This promotes a clear separation of concerns, and maintainability over time.

The project is organized into three main layers:

- **Domain**: contains the core business model and rules, with no dependencies on any framework or external library.
  It defines the entities (`TimeSlot`, `Meeting`), value objects (`TimeRange`, `CalendarEntry`, commands and queries),
  and repository interfaces (ports) that the rest of the application depends on. Nothing outside this layer can corrupt
  the business invariants.

- **Application**: orchestrates the use cases (`CreateTimeSlotUseCase`, `CreateMeetingUseCase`, `DeleteTimeSlotUseCase`,
  `UpdateTimeSlotUseCase`, `SearchTimeSlotsUseCase`, `GetCalendarByOwnerUseCase`). Each use case is a single-responsibility
  class that coordinates domain objects and calls repository ports. It does not know about HTTP, databases, or any
  delivery mechanism.

- **Infrastructure**: implements the ports defined by the domain and wires everything together. It is split into two
  sub-layers:
  - **`persistence`** — JPA entities, Spring Data repositories, and adapters that translate between JPA entities and
    domain objects.
  - **`rest`** — Spring MVC controllers, request/response DTOs, API mappers, OpenAPI configuration, and the global
    exception handler.

This architectural approach ensures that the domain remains the most stable and central part of the system, while the
infrastructure can evolve or change with minimal impact on the core business logic.

---

## Technical decisions

### Why these choices were made

**PostgreSQL as the database**
PostgreSQL was chosen over other relational databases because the core overlap-detection requirement maps directly onto
its `GIST` index and `EXCLUDE USING GIST` constraint — a feature not available in MySQL or standard SQL. The
`tstzrange` range type combined with the `&&` (overlaps) operator lets the database enforce the "no two slots for the
same owner can overlap" invariant atomically and without application-level locking. No other widely available
open-source database offers this combination out of the box.

**UUID v7 as primary key**
Time-ordered UUIDs (via `java-uuid-generator`) are used instead of `UUID.randomUUID()`. Random UUIDs fragment B-tree
indexes on every insert; v7 UUIDs are monotonically increasing, keeping index pages dense. Generation stays in the
application layer (use cases), not in a JPA `@PrePersist`, to preserve hexagonal architecture: the domain ID is known
before the entity ever touches the database.

**PostgreSQL exclusion constraint for overlap detection**
Overlap validation is enforced at the database level with a `GIST` exclusion constraint (`no_overlapping_slots`),
not just in application code. An application-level check alone has a race condition: two concurrent requests can both
pass the check and then both insert. The constraint makes it impossible regardless of concurrency. Because `ddl-auto: update`
does not support exclusion constraints, the constraint is applied programmatically via `DatabaseConstraintInitializer`
on startup (idempotent `DO $$ IF NOT EXISTS ... $$`).

**Optimistic locking on `TimeSlot`**
The `@Version` field on `TimeSlotJPAEntity` protects against lost updates when two meetings try to claim the same slot
concurrently. Combined with the `AND ts.busy = false` condition in `markSlotsAsBusy`, the use case detects a concurrent
modification when the updated row count is lower than expected and throws immediately.

**Batch queries in `CreateMeetingUseCase`**
Participant availability is resolved in a single `findFreeSlotsCoveringForOwners` query (one `IN` clause) instead of one
query per participant. All slots are then marked busy in a single `UPDATE ... WHERE id IN (...)` instead of one save per slot.
This keeps the number of round-trips to the database constant regardless of the number of participants.

**LAZY loading for `meeting_participants`**
The `@ElementCollection` for participants is kept with its default `LAZY` fetch type. Use cases that need the participant
list run within a `@Transactional` context, so the session is open when the collection is accessed. Switching to `EAGER`
would load participants on every query that returns a `Meeting`, even when not needed.

**`data.sql` for seed data**
Seed data is loaded via `data.sql` with `ON CONFLICT (id) DO NOTHING`, making it idempotent and safe to run on every
startup. Chosen over an `ApplicationRunner` bean because it requires no Java code and is a standard Spring Boot mechanism.

**Testcontainers for integration tests**
All integration tests spin up a real PostgreSQL instance via Testcontainers instead of H2 or mocks. This catches
constraint violations, JPQL quirks, and schema issues that in-memory databases or mocks would silently hide.

---

### Left prepared but incomplete

**Grafana dashboards**
Grafana starts with Prometheus already wired as the default datasource, but no dashboards are provisioned. To get a
useful JVM + HTTP dashboard immediately, import the community dashboard **JVM Micrometer (ID 4701)** from Grafana Labs:
Dashboards → Import → enter `4701`.

**Custom metrics**
Only two counters are instrumented (`meetings.created`, `meetings.creation.failed`). The Micrometer + Prometheus stack
is fully wired and ready; adding new counters, gauges, or timers is a one-liner anywhere a `MeterRegistry` is injected.

**`docker` Spring profile**
`docker-compose.yml` sets `SPRING_PROFILES_ACTIVE: docker`, but there is no `application-docker.yml`. The datasource
is configured via environment variables that `application.yaml` already reads with `${SPRING_DATASOURCE_URL:...}`, so
the profile is a placeholder for any future docker-specific overrides (e.g. log format, connection pool size).

---

### Not implemented due to time constraints

**Authentication and authorization**
The API is completely open. A real deployment would need at minimum an API key or JWT validation layer (Spring Security +
OAuth2/OIDC). Ownership of time slots is currently just a free-text `owner` field with no identity verification.

**Database migrations (Flyway / Liquibase)**
`ddl-auto: update` is used for convenience. It is not safe for production: it can silently skip destructive changes and
has no rollback capability. A migration tool should replace it before going to production.

**Rate limiting**
No rate limiting is applied. Under heavy load the overlap-check + insert flow could be a bottleneck; a token-bucket
limiter (e.g. Resilience4j or a gateway) should be added before exposing this publicly.

**Meeting duration vs. time slot duration**
The current model assumes that a meeting occupies the full duration of the owner's time slot. When creating a meeting,
only a `timeSlotId` is provided — no explicit duration — so the meeting is implicitly as long as the slot. This has two
consequences that are currently out of scope:

- *Participant matching ignores duration mismatch.* When looking for a free slot for each participant, the query checks
  that the participant's slot covers the owner's entire time range. A participant with a slot that is long enough for
  the meeting but does not exactly align with the owner's slot boundaries would be incorrectly rejected.
- *No slot splitting.* If a meeting were shorter than the owner's time slot, the remaining free time before or after
  the meeting would be lost. A complete implementation would split the original slot into the meeting slot plus one or
  two residual free slots representing the unused time.

Addressing this properly would require introducing an explicit `duration` field on `CreateMeetingCommand`, adjusting
participant availability queries to check coverage of the meeting window rather than the full slot, and implementing
slot-splitting logic on booking.

**Meeting cancellation**
Once created, a meeting cannot be cancelled. Implementing cancellation would require a `DELETE /api/v1/meetings/{id}`
endpoint that deletes the meeting record and marks all linked time slots back as free — ensuring both operations happen
atomically within the same transaction. The `MeetingRepository` already has `findById` and the time slot port already
has `markSlotsAsBusy`; the inverse `markSlotsAsFree` operation and the endpoint itself would be the missing pieces.

---

## Testing strategy

The test suite is structured in two layers that mirror the production architecture.

**Unit tests** cover the domain and application layers in isolation. Domain tests (`TimeRangeTest`, `TimeSlotTest`,
`MeetingTest`) verify business invariants with no dependencies. Application tests (`CreateTimeSlotUseCaseTest`,
`UpdateTimeSlotUseCaseTest`, `DeleteTimeSlotUseCaseTest`, `SearchTimeSlotsUseCaseTest`, `CreateMeetingUseCaseTest`,
`GetCalendarByOwnerUseCaseTest`) use Mockito to stub the repository ports and assert use case behaviour, happy paths
and all relevant error paths, without touching the database.

**Integration tests** cover the infrastructure layer with a real PostgreSQL instance provided by Testcontainers:
- *Repository adapters* (`TimeSlotJPARepositoryAdapterTest`, `MeetingJPARepositoryAdapterTest`) test the JPA queries,
  the exclusion constraint, and the mapping between JPA entities and domain objects against the actual database schema.
- *REST controllers* (`TimeSlotControllerTest`, `MeetingControllerTest`) exercise the full Spring MVC stack via
  MockMvc, from HTTP request parsing and validation through use case execution to the JSON response shape and HTTP
  status codes.

The decision to use Testcontainers instead of H2 or mocks at the integration level was deliberate: the PostgreSQL
exclusion constraint (`GIST`-based overlap check) and the JPQL bulk update with `@Modifying` are features that an
in-memory database would not replicate faithfully.

To run the full test suite:

```bash
mvn test
```

Testcontainers will pull a PostgreSQL Docker image automatically on the first run — Docker must be running.

**What is not covered:**
- *Contract tests* — no consumer-driven contract tests (e.g. Pact) to verify that clients and the API stay in sync.
- *End-to-end tests* — no tests that drive the running Docker stack; the integration tests cover the same paths but
  within the Spring context.
- *Load / stress tests* — the concurrent slot-booking path (exclusion constraint + optimistic locking) is exercised
  functionally but not under simulated concurrency.