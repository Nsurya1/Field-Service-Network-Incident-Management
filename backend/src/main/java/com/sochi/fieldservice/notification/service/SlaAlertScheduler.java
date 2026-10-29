package com.sochi.fieldservice.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SlaAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaAlertScheduler.class);

    private final SlaAlertService slaAlertService;

    public SlaAlertScheduler(SlaAlertService slaAlertService) {
        this.slaAlertService = slaAlertService;
    }

    @Scheduled(fixedRateString = "${app.sla.alert.fixed-rate-ms:60000}", initialDelayString = "${app.sla.alert.initial-delay-ms:5000}")
    public void runSlaAlertCheck() {
        try {
            log.debug("Starting scheduled SLA alert check...");
            slaAlertService.checkAndGenerateAlerts();
        } catch (Exception e) {
            log.error("Error occurred during scheduled SLA alert check: {}", e.getMessage(), e);
        }
    }
}
