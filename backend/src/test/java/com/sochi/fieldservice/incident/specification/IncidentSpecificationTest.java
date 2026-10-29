package com.sochi.fieldservice.incident.specification;

import com.sochi.fieldservice.incident.dto.IncidentFilterCriteria;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IncidentSpecificationTest {

    @Mock
    private Root<Incident> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<Object> path;

    @Mock
    private Expression<String> stringExpr;

    @Mock
    private Expression<java.sql.Timestamp> dateExpr;

    @Mock
    private Predicate predicate;

    @Test
    void shouldReturnConjunctionWhenCriteriaIsNull() {
        Specification<Incident> spec = IncidentSpecification.withFilter(null);
        when(cb.conjunction()).thenReturn(predicate);

        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).conjunction();
    }

    @Test
    void shouldBuildPredicatesForFullCriteria() {
        IncidentFilterCriteria criteria = new IncidentFilterCriteria();
        criteria.setSearch("switch");
        criteria.setStatus(IncidentStatus.OPEN);
        criteria.setPriority(IncidentPriority.CRITICAL);
        criteria.setAssignedEngineerId(10L);
        criteria.setCreatedFrom(LocalDateTime.now().minusDays(3));
        criteria.setCreatedTo(LocalDateTime.now());
        criteria.setBreachedOnly(true);

        when(root.get(anyString())).thenReturn(path);
        when(path.get(anyString())).thenReturn(path);
        when(cb.lower(any())).thenReturn(stringExpr);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        when(cb.equal(any(), any())).thenReturn(predicate);
        when(cb.greaterThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        when(cb.lessThanOrEqualTo(any(), any(LocalDateTime.class))).thenReturn(predicate);
        when(root.get("status")).thenReturn(path);
        when(path.in(any(IncidentStatus.class), any(IncidentStatus.class))).thenReturn(predicate);
        when(cb.isNotNull(any())).thenReturn(predicate);
        when(cb.greaterThan(any(), any(Expression.class))).thenReturn(predicate);
        when(cb.currentTimestamp()).thenReturn(dateExpr);
        when(cb.not(any(Predicate.class))).thenReturn(predicate);
        when(cb.and(any(), any(), any(), any())).thenReturn(predicate);
        when(cb.and(any(), any(), any())).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Specification<Incident> spec = IncidentSpecification.withFilter(criteria);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb, atLeastOnce()).and(any(Predicate[].class));
    }
}
