package com.rbac.base.modules.ticket.dto;

import lombok.Data;

import java.util.List;

/**
 * 代码生成/修改时间：2026-07-01。
 * 知识库保存入参，结构化承载后续 Agent 可检索内容。
 * 入参：标题、分类、标签、问题现象、原因分析、解决步骤、适用范围、关联工单和附件。
 * 出参：无。
 * 异常场景：标题或解决步骤为空时拒绝保存。
 */
@Data
public class KnowledgeSaveDTO {
    private Long id;
    private String title;
    private String category;
    private String tags;
    private String phenomenon;
    private String causeAnalysis;
    private String solutionSteps;
    private String applicableScope;
    private Long sourceTicketId;
    private List<Long> linkedTicketIds;
    private List<Long> fileIds;
}
