package com.sochi.fieldservice.observability;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.notification.entity.NotificationType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessMetricsServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    private MeterRegistry meterRegistry;
    private BusinessMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new BusinessMetricsService(meterRegistry, incidentRepository);
    }

    @Test
    void shouldRegisterAndComputeGauges() {
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(4L);
        when(incidentRepository.countByStatus(IncidentStatus.ASSIGNED)).thenReturn(3L);
        when(incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS)).thenReturn(2L);
        when(incidentRepository.countByStatusInAndSlaDeadlineBefore(anyList(), any())).thenReturn(1L);

        double active = metricsService.countActiveIncidents();
        assertThat(active).isEqualTo(9.0);

        double breached = metricsService.countBreachedIncidents();
        assertThat(breached).isEqualTo(1.0);
    }

    @Test
    void shouldHandleExceptionInGaugesGracefully() {
        when(incidentRepository.countByStatus(any())).thenThrow(new RuntimeException("DB offline"));
        when(incidentRepository.countByStatusInAndSlaDeadlineBefore(any(), any())).thenThrow(new RuntimeException("DB offline"));

        assertThat(metricsService.countActiveIncidents()).isEqualTo(0.0);
        assertThat(metricsService.countBreachedIncidents()).isEqualTo(0.0);
    }

    @Test
    void shouldRecordIncidentCreatedCounter() {
        metricsService.recordIncidentCreated(IncidentPriority.CRITICAL);
        metricsService.recordIncidentCreated(IncidentPriority.CRITICAL);
        metricsService.recordIncidentCreated(null);

        Counter criticalCounter = meterRegistry.find("incident.created.total")
                .tag("priority", "CRITICAL")
                .counter();
        assertThat(criticalCounter).isNotNull();
        assertThat(criticalCounter.count()).isEqualTo(2.0);

        Counter unknownCounter = meterRegistry.find("incident.created.total")
                .tag("priority", "UNKNOWN")
                .counter();
        assertThat(unknownCounter).isNotNull();
        assertThat(unknownCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordIncidentResolvedCounter() {
        metricsService.recordIncidentResolved(IncidentPriority.HIGH);
        metricsService.recordIncidentResolved(null);

        Counter highCounter = meterRegistry.find("incident.resolved.total")
                .tag("priority", "HIGH")
                .counter();
        assertThat(highCounter).isNotNull();
        assertThat(highCounter.count()).isEqualTo(1.0);

        Counter unknownCounter = meterRegistry.find("incident.resolved.total")
                .tag("priority", "UNKNOWN")
                .counter();
        assertThat(unknownCounter).isNotNull();
        assertThat(unknownCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordSlaAlertGeneratedCounter() {
        metricsService.recordSlaAlertGenerated(NotificationType.WARNING);
        metricsService.recordSlaAlertGenerated(NotificationType.BREACHED);
        metricsService.recordSlaAlertGenerated(null);

        Counter warningCounter = meterRegistry.find("sla.alert.generated.total")
                .tag("type", "WARNING")
                .counter();
        assertThat(warningCounter).isNotNull();
        assertThat(warningCounter.count()).isEqualTo(1.0);

        Counter breachedCounter = meterRegistry.find("sla.alert.generated.total")
                .tag("type", "BREACHED")
                .counter();
        assertThat(breachedCounter).isNotNull();
        assertThat(breachedCounter.count()).isEqualTo(1.0);

        Counter unknownCounter = meterRegistry.find("sla.alert.generated.total")
                .tag("type", "UNKNOWN")
                .counter();
        assertThat(unknownCounter).isNotNull();
        assertThat(unknownCounter.count()).isEqualTo(1.0);
    }
}
