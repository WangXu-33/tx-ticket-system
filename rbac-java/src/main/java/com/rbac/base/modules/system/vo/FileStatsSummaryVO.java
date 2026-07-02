package com.rbac.base.modules.system.vo;

import lombok.Data;

@Data
public class FileStatsSummaryVO {
    private Long totalCount;
    private Long totalSize;
    private Long todayCount;
    private Long todaySize;
}
