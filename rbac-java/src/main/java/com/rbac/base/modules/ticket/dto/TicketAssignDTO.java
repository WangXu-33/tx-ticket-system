package com.rbac.base.modules.ticket.dto;

import lombok.Data;

/**
 * 代码生成/修改时间：2026-07-01。
 * 工单分派/转派入参，记录目标处理人和业务原因。
 * 入参：工单 ID、目标处理人 ID、分派原因。
 * 出参：无。
 * 异常场景：目标处理人不存在时拒绝操作。
 */
@Data
public class TicketAssignDTO {
    private Long ticketId;
    private Long handlerId;
    private String reason;
}
