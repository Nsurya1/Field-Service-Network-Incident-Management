package com.sochi.fieldservice.database;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class DatabaseController {

    private  final JdbcTemplate jdbcTemplate;

    public  DatabaseController(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("api/v1/database-check")
public Map<String, Object> checkDatabase(){
    String databaseName = jdbcTemplate.queryForObject(
            "SELECT current_database()",
            String.class
    );

    return Map.of(
            "database",databaseName,
            "connected", true
    );
    }
}
