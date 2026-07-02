package com.rbac.base.modules.system.vo;

import lombok.Data;

@Data
public class ServerMetricsVO {
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Long jvmUsed;
    private Long jvmMax;
}
