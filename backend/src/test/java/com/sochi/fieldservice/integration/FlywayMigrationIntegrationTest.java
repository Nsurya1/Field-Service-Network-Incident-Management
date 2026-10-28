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
    @DisplayName("Verify Flyway migrations V1 through V5 are applied and registered in test database")
    void shouldApplyAndTrackFlywayMigrations() {
        assertNotNull(flyway, "Flyway bean must be initialized by Spring Boot");

        MigrationInfo[] applied = flyway.info().applied();
        assertTrue(applied.length >= 5, "Migrations V1 through V5 must be applied");

        List<String> versions = Arrays.stream(applied)
                .map(info -> info.getVersion().getVersion())
                .toList();

        assertTrue(versions.contains("1"), "Migration V1 baseline must be recorded");
        assertTrue(versions.contains("2"), "Migration V2 timestamp extension must be recorded");
        assertTrue(versions.contains("3"), "Migration V3 link user to engineer must be recorded");
        assertTrue(versions.contains("4"), "Migration V4 version and locking must be recorded");
        assertTrue(versions.contains("5"), "Migration V5 engineer fields must be recorded");

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
        assertTrue(count >= 5, "flyway_schema_history must have at least 5 successful records");

        // Verify engineer_id column exists on users table
        Integer colCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'engineer_id'",
                Integer.class
        );
        assertNotNull(colCount);
        assertEquals(1, colCount, "users table must contain engineer_id column");

        // Verify version column exists on incident table
        Integer versionColCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'incident' AND column_name = 'version'",
                Integer.class
        );
        assertNotNull(versionColCount);
        assertEquals(1, versionColCount, "incident table must contain version column");

        // Verify phone, active, and availability_status columns exist on engineers table
        Integer phoneCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'engineers' AND column_name = 'phone'",
                Integer.class
        );
        assertEquals(1, phoneCount, "engineers table must contain phone column");

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'engineers' AND column_name = 'active'",
                Integer.class
        );
        assertEquals(1, activeCount, "engineers table must contain active column");

        Integer availCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'engineers' AND column_name = 'availability_status'",
                Integer.class
        );
        assertEquals(1, availCount, "engineers table must contain availability_status column");
    }
}
