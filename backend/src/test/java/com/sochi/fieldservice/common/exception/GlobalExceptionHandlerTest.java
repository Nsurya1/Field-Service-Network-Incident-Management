package com.sochi.fieldservice.common.exception;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleResponseStatusException() {
        ResponseStatusException exception =
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Incident not found"
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatusException(exception);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals(
                "Incident not found",
                response.getBody().getMessage()
        );
    }

    @Test
    void shouldUseDefaultMessageWhenReasonIsNull() {
        ResponseStatusException exception =
                new ResponseStatusException(HttpStatus.NOT_FOUND);

        ResponseEntity<ErrorResponse> response =
                handler.handleResponseStatusException(exception);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                response.getBody().getMessage()
        );
    }

    @Test
    void shouldHandleValidationException() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        new Object(),
                        "request"
                );

        bindingResult.addError(
                new FieldError(
                        "request",
                        "title",
                        "Title is required"
                )
        );

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        null,
                        bindingResult
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleValidationException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals(
                "title: Title is required",
                response.getBody().getMessage()
        );
    }

    @Test
    void shouldUseFallbackMessageWhenNoValidationErrorsExist() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        new Object(),
                        "request"
                );

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        null,
                        bindingResult
                );

        ResponseEntity<ErrorResponse> response =
                handler.handleValidationException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(
                "Request validation failed",
                response.getBody().getMessage()
        );
    }
}
