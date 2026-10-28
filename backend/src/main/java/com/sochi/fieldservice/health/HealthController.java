package com.sochi.fieldservice.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, String > health(){
        return Map.of(
                "status", "UP",
                "application","Field Service Managment"
        );
    }
    @GetMapping("/api/v1")
    public Map<String, String> welcome() {
        return Map.of(
                "project", "Field Service & Network Incident Management System",
                "version", "1.0.0",
                "status", "Development"
        );
    }
}
