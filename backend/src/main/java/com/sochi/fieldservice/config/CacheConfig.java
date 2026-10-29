package com.sochi.fieldservice.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_INCIDENT_STATS = "incidentStats";
    public static final String CACHE_AVAILABLE_ENGINEERS = "availableEngineers";
    public static final String CACHE_ENGINEER_LIST = "engineerList";
    public static final String CACHE_REPORT_SUMMARY = "reportSummary";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                CACHE_INCIDENT_STATS,
                CACHE_AVAILABLE_ENGINEERS,
                CACHE_ENGINEER_LIST,
                CACHE_REPORT_SUMMARY
        );
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return cacheManager;
    }
}
