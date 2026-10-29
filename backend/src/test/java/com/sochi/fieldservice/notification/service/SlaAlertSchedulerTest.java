package com.sochi.fieldservice.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaAlertSchedulerTest {

    @Mock
    private SlaAlertService slaAlertService;

    @InjectMocks
    private SlaAlertScheduler slaAlertScheduler;

    @Test
    void shouldCallSlaAlertServiceOnSchedule() {
        slaAlertScheduler.runSlaAlertCheck();
        verify(slaAlertService, times(1)).checkAndGenerateAlerts();
    }

    @Test
    void shouldHandleExceptionGracefully() {
        when(slaAlertService.checkAndGenerateAlerts()).thenThrow(new RuntimeException("Database down"));

        // Should not throw
        slaAlertScheduler.runSlaAlertCheck();

        verify(slaAlertService, times(1)).checkAndGenerateAlerts();
    }
}
