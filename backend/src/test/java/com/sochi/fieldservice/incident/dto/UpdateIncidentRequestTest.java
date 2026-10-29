package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateIncidentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldValidateValidUpdateIncidentRequest() {
        UpdateIncidentRequest request = new UpdateIncidentRequest(
                "Router Failure",
                "Core router in Rack 4 is down",
                "Indore DC",
                IncidentPriority.CRITICAL
        );

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.getTitle()).isEqualTo("Router Failure");
        assertThat(request.getDescription()).isEqualTo("Core router in Rack 4 is down");
        assertThat(request.getLocation()).isEqualTo("Indore DC");
        assertThat(request.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
    }

    @Test
    void shouldFailValidationOnBlankOrNullFields() {
        UpdateIncidentRequest request = new UpdateIncidentRequest("", "", "", null);
        assertThat(validator.validate(request)).hasSize(4);
    }

    @Test
    void shouldSupportIncidentFilterCriteriaGettersAndSetters() {
        LocalDateTime from = LocalDateTime.now().minusDays(2);
        LocalDateTime to = LocalDateTime.now();

        IncidentFilterCriteria criteria = new IncidentFilterCriteria(
                "router",
                IncidentStatus.OPEN,
                IncidentPriority.HIGH,
                5L,
                from,
                to,
                true
        );

        assertThat(criteria.getSearch()).isEqualTo("router");
        assertThat(criteria.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(criteria.getPriority()).isEqualTo(IncidentPriority.HIGH);
        assertThat(criteria.getAssignedEngineerId()).isEqualTo(5L);
        assertThat(criteria.getCreatedFrom()).isEqualTo(from);
        assertThat(criteria.getCreatedTo()).isEqualTo(to);
        assertThat(criteria.getBreachedOnly()).isTrue();

        criteria.setSearch("switch");
        criteria.setStatus(IncidentStatus.ASSIGNED);
        criteria.setPriority(IncidentPriority.LOW);
        criteria.setAssignedEngineerId(10L);
        criteria.setCreatedFrom(null);
        criteria.setCreatedTo(null);
        criteria.setBreachedOnly(false);

        assertThat(criteria.getSearch()).isEqualTo("switch");
        assertThat(criteria.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(criteria.getPriority()).isEqualTo(IncidentPriority.LOW);
        assertThat(criteria.getAssignedEngineerId()).isEqualTo(10L);
        assertThat(criteria.getCreatedFrom()).isNull();
        assertThat(criteria.getCreatedTo()).isNull();
        assertThat(criteria.getBreachedOnly()).isFalse();
    }
}
