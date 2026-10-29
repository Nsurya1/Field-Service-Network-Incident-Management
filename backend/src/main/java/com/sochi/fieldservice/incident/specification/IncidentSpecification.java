package com.sochi.fieldservice.incident.specification;

import com.sochi.fieldservice.incident.dto.IncidentFilterCriteria;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class IncidentSpecification {

    private IncidentSpecification() {
    }

    public static Specification<Incident> withFilter(IncidentFilterCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            // Free-text search on title, description, location (case-insensitive)
            if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                String pattern = "%" + criteria.getSearch().trim().toLowerCase() + "%";
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), pattern);
                Predicate locPredicate = cb.like(cb.lower(root.get("location")), pattern);
                predicates.add(cb.or(titlePredicate, descPredicate, locPredicate));
            }

            // Status filter
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            // Priority filter
            if (criteria.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), criteria.getPriority()));
            }

            // Assigned Engineer filter
            if (criteria.getAssignedEngineerId() != null) {
                predicates.add(cb.equal(root.get("assignedEngineer").get("id"), criteria.getAssignedEngineerId()));
            }

            // CreatedFrom filter (inclusive)
            if (criteria.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedFrom()));
            }

            // CreatedTo filter (inclusive)
            if (criteria.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedTo()));
            }

            // BreachedOnly filter
            if (Boolean.TRUE.equals(criteria.getBreachedOnly())) {
                Predicate resolvedOrClosed = root.get("status").in(IncidentStatus.RESOLVED, IncidentStatus.CLOSED);

                Predicate resolvedBreached = cb.and(
                        resolvedOrClosed,
                        cb.isNotNull(root.get("resolvedAt")),
                        cb.isNotNull(root.get("slaDeadline")),
                        cb.greaterThan(root.get("resolvedAt"), root.get("slaDeadline"))
                );

                Predicate activeBreached = cb.and(
                        cb.not(resolvedOrClosed),
                        cb.isNotNull(root.get("slaDeadline")),
                        cb.greaterThan(cb.currentTimestamp(), root.get("slaDeadline"))
                );

                predicates.add(cb.or(resolvedBreached, activeBreached));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
