package com.sochi.fieldservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    void shouldInitializeCacheManagerWithConfiguredCaches() {
        CacheConfig config = new CacheConfig();
        CacheManager cacheManager = config.cacheManager();

        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder(
                CacheConfig.CACHE_INCIDENT_STATS,
                CacheConfig.CACHE_AVAILABLE_ENGINEERS,
                CacheConfig.CACHE_ENGINEER_LIST,
                CacheConfig.CACHE_REPORT_SUMMARY
        );

        Cache statsCache = cacheManager.getCache(CacheConfig.CACHE_INCIDENT_STATS);
        assertThat(statsCache).isNotNull();

        Cache engineerCache = cacheManager.getCache(CacheConfig.CACHE_ENGINEER_LIST);
        assertThat(engineerCache).isNotNull();

        Cache reportCache = cacheManager.getCache(CacheConfig.CACHE_REPORT_SUMMARY);
        assertThat(reportCache).isNotNull();
    }
}
