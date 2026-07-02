package com.rbac.base.modules.log.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String ipaddr;
    private String loginLocation;
    private String browser;
    private String os;
    private Integer status; // 0成功 1失败
    private String msg;
    private LocalDateTime loginTime;
}
