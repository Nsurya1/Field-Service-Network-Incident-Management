package com.sochi.fieldservice.report.repository;

import java.time.LocalDate;

public interface DailyCountProjection {
    LocalDate getDay();
    Long getCount();
}
