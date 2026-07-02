package com.rbac.base.modules.ticket.dto;

import lombok.Data;

import java.util.List;

/**
 * 代码生成/修改时间：2026-07-01。
 * 工单解决入参，保存最终解决方案和附件。
 * 入参：工单 ID、解决方案、附件 ID。
 * 出参：无。
 * 异常场景：解决方案为空时拒绝标记解决。
 */
@Data
public class TicketResolveDTO {
    private Long ticketId;
    private String solution;
    private List<Long> fileIds;
}
