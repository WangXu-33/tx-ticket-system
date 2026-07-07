package com.rbac.base.modules.ticket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.modules.ticket.config.TicketAiProperties;
import com.rbac.base.modules.ticket.dto.TicketAiRequestDTO;
import com.rbac.base.modules.ticket.dto.TicketAiSuggestionDTO;
import com.rbac.base.modules.ticket.entity.KnowledgeArticle;
import com.rbac.base.modules.ticket.entity.Ticket;
import com.rbac.base.modules.ticket.mapper.KnowledgeArticleMapper;
import com.rbac.base.modules.ticket.mapper.TicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 代码生成/修改时间：2026-07-05。
 * 功能说明：工单 AI/Agent 建议服务，当前提供本地规则兜底，后续可替换为 Spring AI 或 LangChain4j 适配器。
 * 入参：TicketAiRequestDTO。
 * 出参：TicketAiSuggestionDTO。
 * 异常场景：外部 AI 未启用、key 未配置或上下文不足时返回可用的规则建议，不阻断工单流程。
 */
@Service
public class TicketAiService {
    private static final String STATUS_PUBLISHED = "published";
    private static final String TODO_INTEGRATION = "TODO(agent): 选择 Spring AI 或 LangChain4j 后，在此服务内替换 local-rule 规则实现；需要配置 TICKET_AI_API_KEY、TICKET_AI_BASE_URL、TICKET_AI_MODEL。";

    private final TicketMapper ticketMapper;
    private final KnowledgeArticleMapper articleMapper;
    private final TicketAiProperties properties;

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：注入工单、知识库和 AI 配置依赖。
     * 入参：TicketMapper、KnowledgeArticleMapper、TicketAiProperties。
     * 出参：TicketAiService 实例。
     * 异常场景：依赖缺失时由 Spring 容器启动阶段报错。
     */
    public TicketAiService(TicketMapper ticketMapper,
                           KnowledgeArticleMapper articleMapper,
                           TicketAiProperties properties) {
        this.ticketMapper = ticketMapper;
        this.articleMapper = articleMapper;
        this.properties = properties;
    }

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：提单前预整理客户原文，给出标题、分类、优先级、结构化描述和审批关注点。
     * 入参：客户原始描述和当前表单上下文。
     * 出参：可直接回填前端表单的建议。
     * 异常场景：文本为空时返回基础补全模板。
     */
    public TicketAiSuggestionDTO precheck(TicketAiRequestDTO request) {
        String text = normalizeText(firstText(request == null ? null : request.getRawText(),
                request == null ? null : request.getDescription(),
                request == null ? null : request.getContent()));
        TicketAiSuggestionDTO suggestion = baseSuggestion(request, text);
        suggestion.setTitle(firstText(request == null ? null : request.getTitle(), inferTitle(text)));
        suggestion.setDescription(buildSubmitDescription(text, suggestion));
        suggestion.setSummary(buildSubmitSummary(suggestion));
        suggestion.setNextActions(List.of(
                "补齐影响范围、开始时间、涉及部门和是否持续复现",
                "上传报错截图、日志片段或录屏，便于审批人与运维判断",
                "高优先级工单需补充业务影响、回滚方式和期望完成时间"
        ));
        return suggestion;
    }

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：处理阶段生成排查建议、下一步动作和可参考知识库条目。
     * 入参：工单 ID 或工单描述上下文。
     * 出参：排查清单、解决草稿基础结构和知识候选。
     * 异常场景：工单不存在时退化为请求文本建议。
     */
    public TicketAiSuggestionDTO diagnose(TicketAiRequestDTO request) {
        Ticket ticket = request != null && request.getTicketId() != null ? ticketMapper.selectById(request.getTicketId()) : null;
        String text = normalizeText(firstText(
                request == null ? null : request.getContent(),
                request == null ? null : request.getDescription(),
                ticket == null ? null : ticket.getDescription(),
                ticket == null ? null : ticket.getTitle()));
        TicketAiSuggestionDTO suggestion = baseSuggestion(request, text);
        if (ticket != null) {
            suggestion.setTitle(ticket.getTitle());
            suggestion.setCategory(firstText(ticket.getCategory(), suggestion.getCategory()));
            suggestion.setPriority(firstText(ticket.getPriority(), suggestion.getPriority()));
        }
        suggestion.setSummary(buildDiagnoseSummary(suggestion));
        suggestion.setNextActions(buildDiagnoseActions(suggestion.getCategory(), text));
        suggestion.setKnowledgeCandidates(findKnowledgeCandidates(suggestion.getCategory(), text));
        return suggestion;
    }

    private TicketAiSuggestionDTO baseSuggestion(TicketAiRequestDTO request, String text) {
        TicketAiSuggestionDTO suggestion = new TicketAiSuggestionDTO();
        suggestion.setProvider(resolveProvider());
        suggestion.setModel(defaultText(properties.getModel(), "local-rules"));
        suggestion.setIntegrationTodo(TODO_INTEGRATION);
        suggestion.setIssueTypes(resolveIssueTypes(request, text));
        suggestion.setCategory(resolveCategory(request, suggestion.getIssueTypes()));
        suggestion.setPriority(resolvePriority(request, text));
        suggestion.setRiskTags(resolveRiskTags(text, suggestion.getIssueTypes(), suggestion.getPriority()));
        suggestion.setSuggestions(buildSuggestions(suggestion.getCategory(), text));
        suggestion.setImpactScope(inferImpactScope(text));
        suggestion.setTriedActions(inferTriedActions(text));
        return suggestion;
    }

    private String resolveProvider() {
        if (properties.isEnabled() && StringUtils.hasText(properties.getProvider())) {
            return properties.getProvider() + "-reserved";
        }
        return "local-rule";
    }

    private List<String> resolveIssueTypes(TicketAiRequestDTO request, String text) {
        List<String> types = new ArrayList<>();
        if (request != null && request.getIssueTypes() != null) {
            request.getIssueTypes().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(types::add);
        }
        if (containsAny(text, "网络", "vpn", "网关", "超时", "ping", "延迟")) {
            types.add("network");
        }
        if (containsAny(text, "服务器", "服务不可用", "cpu", "内存", "磁盘", "宕机")) {
            types.add("server");
        }
        if (containsAny(text, "权限", "账号", "角色", "登录", "认证")) {
            types.add("account");
        }
        if (containsAny(text, "数据", "同步", "缺失", "报表", "统计", "口径")) {
            types.add("data");
        }
        if (containsAny(text, "页面", "流程", "菜单", "报错", "系统")) {
            types.add("system");
        }
        if (types.isEmpty()) {
            types.add("other");
        }
        return types.stream().distinct().toList();
    }

    private String resolveCategory(TicketAiRequestDTO request, List<String> issueTypes) {
        if (request != null && StringUtils.hasText(request.getCategory())) {
            return request.getCategory().trim();
        }
        return issueTypes == null || issueTypes.isEmpty() ? "other" : issueTypes.get(0);
    }

    private String resolvePriority(TicketAiRequestDTO request, String text) {
        if (request != null && StringUtils.hasText(request.getPriority()) && !"normal".equals(request.getPriority())) {
            return request.getPriority().trim();
        }
        if (containsAny(text, "宕机", "全部", "全员", "生产", "瘫痪", "无法登录", "不可用")) {
            return "urgent";
        }
        if (containsAny(text, "多人", "影响", "失败", "超时", "阻塞", "客户投诉")) {
            return "high";
        }
        return request != null && StringUtils.hasText(request.getPriority()) ? request.getPriority().trim() : "normal";
    }

    private List<String> resolveRiskTags(String text, List<String> issueTypes, String priority) {
        List<String> tags = new ArrayList<>();
        if ("urgent".equals(priority) || "high".equals(priority)) {
            tags.add("审批重点关注");
        }
        if (issueTypes.contains("data")) {
            tags.add("数据一致性");
        }
        if (issueTypes.contains("account")) {
            tags.add("权限合规");
        }
        if (containsAny(text, "生产", "财务", "审批", "支付", "客户")) {
            tags.add("业务影响需确认");
        }
        if (tags.isEmpty()) {
            tags.add("普通受理");
        }
        return tags.stream().distinct().toList();
    }

    private List<String> buildSuggestions(String category, String text) {
        List<String> suggestions = new ArrayList<>();
        suggestions.add("确认影响范围、开始时间、复现频率和最近变更");
        suggestions.add("补齐截图、报错文本、日志时间点和已尝试操作");
        switch (defaultText(category, "other")) {
            case "network" -> suggestions.add("检查 DNS、网关、VPN、链路延迟和同网段对比结果");
            case "server" -> suggestions.add("检查 CPU、内存、磁盘、服务进程、端口和最近发布记录");
            case "account" -> suggestions.add("核对账号状态、角色菜单、数据范围和最近授权变更");
            case "data" -> suggestions.add("确认源表、同步任务、统计时间窗、异常样本和修复口径");
            default -> suggestions.add("先复现问题路径，再按系统、账号、网络、数据四类排除");
        }
        if (containsAny(text, "超时", "慢", "卡")) {
            suggestions.add("记录慢请求时间点，关联网关日志、应用日志和数据库耗时");
        }
        return suggestions.stream().distinct().limit(5).toList();
    }

    private List<String> buildDiagnoseActions(String category, String text) {
        List<String> actions = new ArrayList<>(buildSuggestions(category, text));
        actions.add("处理结论需包含根因、处理步骤、验证结果和后续预防");
        actions.add("无法当场解决时先写内部留痕，再转派给明确的下一任处理人");
        return actions.stream().distinct().limit(6).toList();
    }

    private List<Map<String, Object>> findKnowledgeCandidates(String category, String text) {
        LambdaQueryWrapper<KnowledgeArticle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeArticle::getStatus, STATUS_PUBLISHED)
                .eq(StringUtils.hasText(category) && !"other".equals(category), KnowledgeArticle::getCategory, category)
                .orderByDesc(KnowledgeArticle::getUsefulCount)
                .orderByDesc(KnowledgeArticle::getId)
                .last("LIMIT 5");
        List<KnowledgeArticle> articles = articleMapper.selectList(wrapper);
        if (articles.isEmpty() && StringUtils.hasText(text)) {
            String keyword = inferKeyword(text);
            LambdaQueryWrapper<KnowledgeArticle> fallback = new LambdaQueryWrapper<>();
            fallback.eq(KnowledgeArticle::getStatus, STATUS_PUBLISHED)
                    .and(StringUtils.hasText(keyword), item -> item.like(KnowledgeArticle::getTitle, keyword)
                            .or()
                            .like(KnowledgeArticle::getTags, keyword))
                    .orderByDesc(KnowledgeArticle::getUsefulCount)
                    .last("LIMIT 5");
            articles = articleMapper.selectList(fallback);
        }
        return articles.stream().map(this::toKnowledgeCandidate).toList();
    }

    private Map<String, Object> toKnowledgeCandidate(KnowledgeArticle article) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", article.getId());
        data.put("title", article.getTitle());
        data.put("category", article.getCategory());
        data.put("tags", article.getTags());
        data.put("usefulCount", article.getUsefulCount());
        return data;
    }

    private String buildSubmitDescription(String text, TicketAiSuggestionDTO suggestion) {
        return List.of(
                "1. 问题现象：" + defaultText(text, "待补充客户原始描述"),
                "2. 影响范围：" + defaultText(suggestion.getImpactScope(), "待补充"),
                "3. 已尝试操作：" + defaultText(suggestion.getTriedActions(), "待补充"),
                "4. 期望结果：请运维协助定位并恢复业务"
        ).stream().collect(java.util.stream.Collectors.joining("\n"));
    }

    private String buildSubmitSummary(TicketAiSuggestionDTO suggestion) {
        return "建议分类：" + suggestion.getCategory() + "，建议优先级：" + suggestion.getPriority() + "，审批关注：" + String.join("、", suggestion.getRiskTags());
    }

    private String buildDiagnoseSummary(TicketAiSuggestionDTO suggestion) {
        return "建议按 " + suggestion.getCategory() + " 类问题处理，优先级 " + suggestion.getPriority() + "，先确认影响范围、证据和最近变更。";
    }

    private String inferTitle(String text) {
        if (!StringUtils.hasText(text)) {
            return "待补充问题标题";
        }
        String firstLine = text.split("\\R", 2)[0].trim();
        return firstLine.length() > 32 ? firstLine.substring(0, 32) + "..." : firstLine;
    }

    private String inferImpactScope(String text) {
        if (containsAny(text, "全员", "全部", "多人")) {
            return "多人或全员受影响，需确认具体部门和人数";
        }
        if (containsAny(text, "部门", "销售", "财务", "仓库", "客户")) {
            return "涉及具体业务部门，需补充人数和业务影响";
        }
        return "";
    }

    private String inferTriedActions(String text) {
        if (containsAny(text, "重试", "刷新", "重启", "切换网络", "清缓存")) {
            return "客户已尝试基础恢复操作，需运维继续定位";
        }
        return "";
    }

    private String inferKeyword(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ").trim();
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        String[] parts = normalized.split("\\s+");
        return parts.length == 0 ? "" : parts[0];
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String firstText(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String normalizeText(String value) {
        return defaultText(value, "").replace("\r\n", "\n").trim();
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
