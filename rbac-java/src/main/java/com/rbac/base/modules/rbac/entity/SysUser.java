package com.rbac.base.modules.rbac.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.rbac.base.core.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptId;
    private String username;
    private String password;
    private String nickname;
    private Long avatarFileId;
    private String email;
    private String phone;
    private Integer sex;
    private LocalDate birthDate;
    private String address;
    private String remark;
    private Integer status;
}
