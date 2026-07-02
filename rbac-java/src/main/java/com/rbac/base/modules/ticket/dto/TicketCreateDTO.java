package com.rbac.base.modules.ticket.dto;

import lombok.Data;

import java.util.List;

/**
 * 代码生成/修改时间：2026-07-01。
 * 工单创建入参，承载客户提交的问题内容和附件 ID。
 * 入参：标题、描述、分类、优先级、联系方式、附件 ID。
 * 出参：无。
 * 异常场景：标题或描述为空时拒绝创建。
 */
@Data
public class TicketCreateDTO {
    private String title;
    private String description;
    private String category;
    private String priority;
    private String contactPhone;
    private String contactEmail;
    private List<Long> fileIds;
}
