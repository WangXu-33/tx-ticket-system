package com.rbac.base.modules.system.vo;

import lombok.Data;

@Data
public class DashboardOverviewVO {
    private Long userTotal;
    private Long enabledUserTotal;
    private Long deptTotal;
    private Long roleTotal;
    private Long menuTotal;
    private Long permissionTotal;
    private Long fileTotal;
    private Long fileTotalSize;
    private Long todayLoginTotal;
    private Long todayOperTotal;
    private Long onlineUserTotal;
}
