package com.doodle.calendar_challenge.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConstraintInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Applying database constraints");
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS btree_gist");
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'no_overlapping_slots'
                    ) THEN
                        ALTER TABLE time_slots
                            ADD CONSTRAINT no_overlapping_slots
                            EXCLUDE USING GIST (
                                owner WITH =,
                                tstzrange(start_at, end_at, '[)') WITH &&
                            );
                    END IF;
                END $$
                """);
        log.info("Database constraints applied successfully");
    }
}
