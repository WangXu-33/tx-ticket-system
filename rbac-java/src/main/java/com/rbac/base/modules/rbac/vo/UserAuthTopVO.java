package com.rbac.base.modules.rbac.vo;

import lombok.Data;

@Data
public class UserAuthTopVO {
    private Long userId;
    private String nickname;
    private Long permissionCount;
}
