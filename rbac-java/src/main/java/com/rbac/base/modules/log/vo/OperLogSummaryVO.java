package com.rbac.base.modules.log.vo;

import lombok.Data;

@Data
public class OperLogSummaryVO {
    private Long totalCount;
    private Long successCount;
    private Long failCount;
    private Long todayCount;
    private Long avgCostTime;
}
