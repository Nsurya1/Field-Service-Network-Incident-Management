package com.sochi.fieldservice.observability;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.notification.entity.NotificationType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BusinessMetricsService {

    private final MeterRegistry meterRegistry;
    private final IncidentRepository incidentRepository;

    public BusinessMetricsService(MeterRegistry meterRegistry, IncidentRepository incidentRepository) {
        this.meterRegistry = meterRegistry;
        this.incidentRepository = incidentRepository;

        registerGauges();
    }

    private void registerGauges() {
        Gauge.builder("incident.active.gauge", this, BusinessMetricsService::countActiveIncidents)
                .description("Current number of active (unresolved) incidents")
                .register(meterRegistry);

        Gauge.builder("incident.breached.gauge", this, BusinessMetricsService::countBreachedIncidents)
                .description("Current number of active incidents past SLA deadline")
                .register(meterRegistry);
    }

    public double countActiveIncidents() {
        try {
            long open = incidentRepository.countByStatus(IncidentStatus.OPEN);
            long assigned = incidentRepository.countByStatus(IncidentStatus.ASSIGNED);
            long inProgress = incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS);
            return (double) (open + assigned + inProgress);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double countBreachedIncidents() {
        try {
            return (double) incidentRepository.countByStatusInAndSlaDeadlineBefore(
                    List.of(IncidentStatus.OPEN, IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS),
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            return 0.0;
        }
    }

    public void recordIncidentCreated(IncidentPriority priority) {
        String priorityTag = priority != null ? priority.name() : "UNKNOWN";
        Counter.builder("incident.created.total")
                .tag("priority", priorityTag)
                .description("Total number of incidents created")
                .register(meterRegistry)
                .increment();
    }

    public void recordIncidentResolved(IncidentPriority priority) {
        String priorityTag = priority != null ? priority.name() : "UNKNOWN";
        Counter.builder("incident.resolved.total")
                .tag("priority", priorityTag)
                .description("Total number of incidents resolved")
                .register(meterRegistry)
                .increment();
    }

    public void recordSlaAlertGenerated(NotificationType type) {
        String typeTag = type != null ? type.name() : "UNKNOWN";
        Counter.builder("sla.alert.generated.total")
                .tag("type", typeTag)
                .description("Total number of SLA alerts generated")
                .register(meterRegistry)
                .increment();
    }
}
