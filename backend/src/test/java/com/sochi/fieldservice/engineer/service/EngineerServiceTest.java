
package com.sochi.fieldservice.engineer.service;

import com.sochi.fieldservice.engineer.dto.CreateEngineerRequest;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EngineerServiceTest {

    @Mock
    private EngineerRepository engineerRepository;

    @InjectMocks
    private EngineerService engineerService;

    @Test
    void createEngineerShouldThrow409WhenEmailAlreadyExists() {
        CreateEngineerRequest request = new CreateEngineerRequest();
        request.setName("Test Engineer");
        request.setEmail("TEST@example.com");
        request.setSpecialization("Network Support");

        when(engineerRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> engineerService.createEngineer(request)
        );

        assertEquals(
                HttpStatus.CONFLICT.value(),
                exception.getStatusCode().value()
        );

        assertEquals(
                "An engineer with this email already exists",
                exception.getReason()
        );

        verify(engineerRepository).existsByEmail("test@example.com");
        verify(engineerRepository, never()).save(any(Engineer.class));
    }


    @Test
    void createEngineerShouldSaveEngineerWhenEmailIsNew() {
        CreateEngineerRequest request = new CreateEngineerRequest();
        request.setName("Test Engineer");
        request.setEmail("TEST@example.com");
        request.setSpecialization("Network Support");

        when(engineerRepository.existsByEmail("test@example.com"))
                .thenReturn(false);

        when(engineerRepository.save(any(Engineer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Engineer result = engineerService.createEngineer(request);

        assertEquals("Test Engineer", result.getName());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Network Support", result.getSpecialization());

        verify(engineerRepository).existsByEmail("test@example.com");
        verify(engineerRepository).save(any(Engineer.class));
    }


    @Test
    void shouldReturnAllEngineers() {
        // Arrange
        Engineer engineer1 = new Engineer();
        engineer1.setName("Rahul Sharma");
        engineer1.setEmail("rahul@example.com");
        engineer1.setSpecialization("Networking");

        Engineer engineer2 = new Engineer();
        engineer2.setName("Amit Verma");
        engineer2.setEmail("amit@example.com");
        engineer2.setSpecialization("Hardware");

        when(engineerRepository.findAll())
                .thenReturn(List.of(engineer1, engineer2));

        // Act
        List<Engineer> engineers = engineerService.getAllEngineers();

        // Assert
        assertEquals(2, engineers.size());
        assertEquals("Rahul Sharma", engineers.get(0).getName());
        assertEquals("Amit Verma", engineers.get(1).getName());

        verify(engineerRepository).findAll();
    }


    @Test
    void shouldReturnEngineerById() {
        // Arrange
        Engineer engineer = new Engineer();
        engineer.setName("Rahul Sharma");
        engineer.setEmail("rahul@example.com");
        engineer.setSpecialization("Networking");

        when(engineerRepository.findById(1L))
                .thenReturn(Optional.of(engineer));

        // Act
        Engineer result = engineerService.getEngineerById(1L);

        // Assert
        assertEquals("Rahul Sharma", result.getName());
        assertEquals("rahul@example.com", result.getEmail());

        verify(engineerRepository).findById(1L);
    }

    @Test
    void shouldThrow404WhenEngineerDoesNotExist() {
        // Arrange
        when(engineerRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act + Assert
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> engineerService.getEngineerById(999L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Engineer not found with ID: 999", exception.getReason());

        verify(engineerRepository).findById(999L);
    }

}
