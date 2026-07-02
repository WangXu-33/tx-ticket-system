package com.rbac.base.modules.notice.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeMyVO {
    private Long id;
    private String title;
    private String content;
    private String noticeType;
    private String priority;
    private LocalDateTime publishTime;
    private Integer readFlag;
    private LocalDateTime readTime;
    private LocalDateTime receiveTime;
}
