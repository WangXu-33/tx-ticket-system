package com.rbac.base.modules.ticket.dto;

import lombok.Data;

/**
 * 代码生成/修改时间：2026-07-04。
 * 功能说明：工单通用动作入参，承载审批、退回、撤销、重开、评价和 SLA 提醒等轻量流程动作。
 * 入参：工单 ID、动作说明、评价分值和 SLA 提醒类型。
 * 出参：无。
 * 异常场景：业务服务会校验工单状态、操作权限和必填内容。
 */
@Data
public class TicketActionDTO {
    private Long ticketId;
    private String content;
    private Integer rating;
    private String slaType;
}
