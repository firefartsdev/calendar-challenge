package com.doodle.calendar_challenge.infrastructure.persistence.timeslot;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "time_slots", indexes = {
        @Index(name = "idx_ts_owner_start_end", columnList = "owner, startAt, endAt"),
        @Index(name = "idx_ts_owner_busy_start_end", columnList = "owner, busy, startAt, endAt")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeSlotJPAEntity {

    @Id
    private UUID id;

    private String owner;

    private Instant startAt;

    private Instant endAt;

    private boolean busy;

    private UUID meetingId;

    @Version
    private Long version;

}
