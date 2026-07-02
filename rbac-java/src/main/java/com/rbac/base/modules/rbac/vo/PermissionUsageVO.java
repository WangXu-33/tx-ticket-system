package com.rbac.base.modules.rbac.vo;

import lombok.Data;

@Data
public class PermissionUsageVO {
    private String permKey;
    private String permName;
    private Long menuBindCount;
    private Long roleBindCount;
    private Long userBindCount;
}
