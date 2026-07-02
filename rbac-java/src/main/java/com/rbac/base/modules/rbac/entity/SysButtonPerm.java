package com.rbac.base.modules.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rbac.base.core.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_button_perm")
public class SysButtonPerm extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long menuId;
    private String name;
    private String permKey;
}
