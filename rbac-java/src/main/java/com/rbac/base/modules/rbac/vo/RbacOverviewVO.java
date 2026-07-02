package com.rbac.base.modules.rbac.vo;

import lombok.Data;

@Data
public class RbacOverviewVO {
    private Long userTotal;
    private Long roleTotal;
    private Long deptTotal;
    private Long menuTotal;
    private Long permissionTotal;
    private Long directGrantUserTotal;
}
