package com.rbac.base.modules.ticket.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.ticket.dto.TicketAiRequestDTO;
import com.rbac.base.modules.ticket.service.TicketAiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代码生成/修改时间：2026-07-05。
 * 功能说明：工单 AI/Agent 预留接口，当前返回本地规则建议，后续可接入真实大模型服务。
 * 入参：TicketAiRequestDTO。
 * 出参：统一 Result 包装的 TicketAiSuggestionDTO。
 * 异常场景：AI 服务未配置时仍返回本地规则建议，不阻塞提单和处理。
 */
@RestController
@RequestMapping("/ticket/ai")
public class TicketAiController {
    private final TicketAiService ticketAiService;

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：注入工单 AI/Agent 建议服务。
     * 入参：TicketAiService。
     * 出参：TicketAiController 实例。
     * 异常场景：服务缺失时由 Spring 容器启动阶段报错。
     */
    public TicketAiController(TicketAiService ticketAiService) {
        this.ticketAiService = ticketAiService;
    }

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：提单前整理客户原话，生成标题、分类、优先级和结构化描述建议。
     * 入参：客户原话和表单上下文。
     * 出参：AI/规则整理建议。
     * 异常场景：文本不足时返回基础模板。
     */
    @PostMapping("/precheck")
    @SaCheckPermission(value = {"ticket:add", "ticket:my:add", "ticket:list", "ticket:my:list"}, mode = SaMode.OR)
    public Result<?> precheck(@RequestBody TicketAiRequestDTO dto) {
        return Result.success(ticketAiService.precheck(dto));
    }

    /**
     * 代码生成/修改时间：2026-07-05。
     * 功能说明：处理阶段生成排查建议、下一步动作和知识库候选。
     * 入参：工单 ID 或描述上下文。
     * 出参：AI/规则诊断建议。
     * 异常场景：工单不存在时退化为请求文本建议。
     */
    @PostMapping("/diagnose")
    @SaCheckPermission(value = {"ticket:list", "ticket:reply", "ticket:resolve"}, mode = SaMode.OR)
    public Result<?> diagnose(@RequestBody TicketAiRequestDTO dto) {
        return Result.success(ticketAiService.diagnose(dto));
    }
}
