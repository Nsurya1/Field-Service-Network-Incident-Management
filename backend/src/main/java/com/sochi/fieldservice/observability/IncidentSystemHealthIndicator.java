package com.sochi.fieldservice.observability;

import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class IncidentSystemHealthIndicator implements HealthIndicator {

    private final IncidentRepository incidentRepository;
    private final EngineerRepository engineerRepository;

    public IncidentSystemHealthIndicator(
            IncidentRepository incidentRepository,
            EngineerRepository engineerRepository) {
        this.incidentRepository = incidentRepository;
        this.engineerRepository = engineerRepository;
    }

    @Override
    public Health health() {
        try {
            long open = incidentRepository.countByStatus(IncidentStatus.OPEN);
            long assigned = incidentRepository.countByStatus(IncidentStatus.ASSIGNED);
            long inProgress = incidentRepository.countByStatus(IncidentStatus.IN_PROGRESS);
            long activeIncidents = open + assigned + inProgress;

            long breachedIncidents = incidentRepository.countByStatusInAndSlaDeadlineBefore(
                    List.of(IncidentStatus.OPEN, IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS),
                    LocalDateTime.now()
            );

            long totalEngineers = engineerRepository.count();

            return Health.up()
                    .withDetail("systemStatus", "OPERATIONAL")
                    .withDetail("activeIncidents", activeIncidents)
                    .withDetail("breachedIncidents", breachedIncidents)
                    .withDetail("totalEngineers", totalEngineers)
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("systemStatus", "UNAVAILABLE")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
