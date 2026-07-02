package com.rbac.base.modules.rbac.vo;

import lombok.Data;

@Data
public class PermissionGrantVO {
    private Long id;
    private String name;
    private String permKey;
    private Integer status;
    private String menuTitles;
}
