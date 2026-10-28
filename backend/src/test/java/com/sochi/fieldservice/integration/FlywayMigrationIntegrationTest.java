package com.sochi.fieldservice.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify Flyway migrations V1, V2, and V3 are applied and registered in test database")
    void shouldApplyAndTrackFlywayMigrations() {
        assertNotNull(flyway, "Flyway bean must be initialized by Spring Boot");

        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 3, "Migrations V1, V2, and V3 must be applied");

        List<String> versions = Arrays.stream(applied)
                .map(info -> info.getVersion().getVersion())
                .toList();

        assertTrue(versions.contains("1"), "Migration V1 baseline must be recorded");
        assertTrue(versions.contains("2"), "Migration V2 timestamp extension must be recorded");
        assertTrue(versions.contains("3"), "Migration V3 link user to engineer must be recorded");

        // Verify all applied migrations have status SUCCESS
        for (MigrationInfo info : applied) {
            assertTrue(info.getState().isApplied(), "Migration " + info.getVersion() + " must be applied successfully");
        }

        // Verify flyway_schema_history table exists in database
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        );
        assertNotNull(count);
        assertTrue(count >= 3, "flyway_schema_history must have at least 3 successful records");

        // Verify engineer_id column exists on users table
        Integer colCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'engineer_id'",
                Integer.class
        );
        assertNotNull(colCount);
        assertEquals(1, colCount, "users table must contain engineer_id column");
    }
}
