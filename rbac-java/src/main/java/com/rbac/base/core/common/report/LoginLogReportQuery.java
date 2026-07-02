package com.rbac.base.core.common.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogReportQuery {
    private String username;
    private Integer status;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
}
