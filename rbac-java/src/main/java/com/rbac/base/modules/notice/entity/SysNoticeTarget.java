package com.rbac.base.modules.notice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_notice_target")
public class SysNoticeTarget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long noticeId;
    private String targetType;
    private Long targetId;
    private String effect;
}
