package com.rbac.base.core.common.report;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReportRange {
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private LocalDate startDate;
    private LocalDate endDate;
}
