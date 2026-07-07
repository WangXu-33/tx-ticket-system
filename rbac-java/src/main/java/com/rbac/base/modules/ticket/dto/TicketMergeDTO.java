package com.rbac.base.modules.ticket.dto;

import lombok.Data;

import java.util.List;

/**
 * 代码生成/修改时间：2026-07-03。
 * 功能说明：重复工单合并入参，承载主工单、重复工单、合并原因和可选知识库关联。
 * 入参：mainTicketId 为保留继续处理的主工单，duplicateTicketIds 为需要关闭并合并的重复工单。
 * 出参：无。
 * 异常场景：主工单为空、重复工单为空、合并自身或状态不允许合并时拒绝处理。
 */
@Data
public class TicketMergeDTO {
    private Long mainTicketId;
    private List<Long> duplicateTicketIds;
    private String reason;
    private Long articleId;
}
