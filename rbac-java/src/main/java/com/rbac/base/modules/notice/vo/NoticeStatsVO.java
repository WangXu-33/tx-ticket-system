package com.rbac.base.modules.notice.vo;

import lombok.Data;

@Data
public class NoticeStatsVO {
    private Long noticeId;
    private Long recipientTotal;
    private Long readTotal;
    private Long unreadTotal;
}
