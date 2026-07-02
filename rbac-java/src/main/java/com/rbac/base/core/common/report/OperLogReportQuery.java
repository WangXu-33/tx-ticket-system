package com.rbac.base.core.common.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperLogReportQuery {
    private String title;
    private String operName;
    private Integer businessType;
    private Integer status;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
}
