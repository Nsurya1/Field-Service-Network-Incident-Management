package com.sochi.fieldservice.report.repository;

public interface EngineerPerformanceProjection {
    Long getEngineerId();
    String getEngineerName();
    Long getResolvedCount();
    Double getAvgResolutionTimeHours();
}
