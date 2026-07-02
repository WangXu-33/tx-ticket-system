package com.rbac.base.modules.notice.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeListVO {
    private Long id;
    private String title;
    private String noticeType;
    private String priority;
    private Integer status;
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
    private Long recipientTotal;
    private Long readTotal;
    private Long unreadTotal;
}
