package com.rbac.base.modules.notice.dto;

import lombok.Data;

@Data
public class NoticeTargetDTO {
    private String targetType;
    private Long targetId;
    private String effect;
}
