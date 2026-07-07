package com.rbac.base.modules.ticket.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 代码生成/修改时间：2026-07-05。
 * 功能说明：工单 AI/Agent 建议出参，兼容当前本地规则模式和后续真实模型返回。
 * 入参：TicketAiService 根据工单上下文填充。
 * 出参：前端用于回填标题、分类、描述、排查清单、风险标签和知识候选。
 * 异常场景：外部 AI 未启用时 provider 返回 local-rule，并通过 integrationTodo 标明后续接入点。
 */
@Data
public class TicketAiSuggestionDTO {
    private Boolean available = true;
    private String provider;
    private String model;
    private String title;
    private String category;
    private String priority;
    private String summary;
    private String description;
    private String impactScope;
    private String triedActions;
    private List<String> issueTypes = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private List<String> riskTags = new ArrayList<>();
    private List<String> nextActions = new ArrayList<>();
    private List<Map<String, Object>> knowledgeCandidates = new ArrayList<>();
    private String integrationTodo;
}
