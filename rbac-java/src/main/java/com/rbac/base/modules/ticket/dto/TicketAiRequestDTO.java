package com.rbac.base.modules.ticket.dto;

import lombok.Data;

import java.util.List;

/**
 * 代码生成/修改时间：2026-07-05。
 * 功能说明：工单 AI/Agent 请求入参，统一承载提单整理、排查建议和知识推荐所需上下文。
 * 入参：工单 ID、客户原文、标题、分类、优先级、系统编码和问题类型。
 * 出参：传递给 TicketAiService 生成建议。
 * 异常场景：文本为空时服务层返回基础模板建议，不调用外部 AI。
 */
@Data
public class TicketAiRequestDTO {
    private Long ticketId;
    private String scenario;
    private String rawText;
    private String title;
    private String description;
    private String content;
    private String category;
    private String priority;
    private String systemCode;
    private List<String> issueTypes;
}
