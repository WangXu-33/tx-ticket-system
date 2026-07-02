package com.rbac.base.modules.ticket.dto;

import lombok.Data;

import java.util.List;

/**
 * 代码生成/修改时间：2026-07-01。
 * 工单回复入参，支持客户补充资料和运维处理备注。
 * 入参：工单 ID、回复内容、可见范围、目标状态、附件 ID。
 * 出参：无。
 * 异常场景：工单不存在或内容为空时拒绝保存。
 */
@Data
public class TicketReplyDTO {
    private Long ticketId;
    private String content;
    private String visibleScope;
    private String targetStatus;
    private List<Long> fileIds;
}
