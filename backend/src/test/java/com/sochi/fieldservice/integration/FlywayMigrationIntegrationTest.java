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
    @DisplayName("Verify Flyway migrations V1 and V2 are applied and registered in test database")
    void shouldApplyAndTrackFlywayMigrations() {
        assertNotNull(flyway, "Flyway bean must be initialized by Spring Boot");

        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 2, "Both V1 and V2 migrations must be applied");

        List<String> versions = Arrays.stream(applied)
                .map(info -> info.getVersion().getVersion())
                .toList();

        assertTrue(versions.contains("1"), "Migration V1 baseline must be recorded");
        assertTrue(versions.contains("2"), "Migration V2 timestamp extension must be recorded");

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
        assertTrue(count >= 2, "flyway_schema_history must record successful migrations");

        // Verify new columns from V2 exist on incident table
        Integer columnCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'incident' AND column_name IN ('resolved_at', 'closed_at')",
                Integer.class
        );
        assertNotNull(columnCount);
        assertEquals(2, columnCount, "incident table must contain both resolved_at and closed_at columns");
    }
}
