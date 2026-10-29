package com.sochi.fieldservice.observability;

import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentSystemHealthIndicatorTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private EngineerRepository engineerRepository;

    @InjectMocks
    private IncidentSystemHealthIndicator healthIndicator;

    @Test
    void shouldReturnHealthUpWithOperationalDetails() {
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(3L);
        when(incidentRepository.countByStatus(IncidentStatus.ASSIGNED)).thenReturn(2L);
        when(incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS)).thenReturn(1L);
        when(incidentRepository.countByStatusInAndSlaDeadlineBefore(anyList(), any())).thenReturn(1L);
        when(engineerRepository.count()).thenReturn(8L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("systemStatus", "OPERATIONAL");
        assertThat(health.getDetails()).containsEntry("activeIncidents", 6L);
        assertThat(health.getDetails()).containsEntry("breachedIncidents", 1L);
        assertThat(health.getDetails()).containsEntry("totalEngineers", 8L);
    }

    @Test
    void shouldReturnHealthDownWhenRepositoryThrowsException() {
        when(incidentRepository.countByStatus(any())).thenThrow(new RuntimeException("DB Connection Timeout"));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("systemStatus", "UNAVAILABLE");
        assertThat(health.getDetails().get("error").toString()).contains("DB Connection Timeout");
    }
}
