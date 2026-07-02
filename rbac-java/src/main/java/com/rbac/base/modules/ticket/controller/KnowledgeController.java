package com.rbac.base.modules.ticket.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.ticket.dto.KnowledgeSaveDTO;
import com.rbac.base.modules.ticket.service.KnowledgeService;
import org.springframework.web.bind.annotation.*;

/**
 * 代码生成/修改时间：2026-07-01。
 * 知识库接口，提供知识文章维护、发布下架和从工单生成草稿能力。
 * 入参：分页筛选参数、知识库 DTO、文章 ID、来源工单 ID。
 * 出参：统一 Result 响应。
 * 异常场景：权限不足、参数缺失、文章不存在时返回错误响应。
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/list")
    @SaCheckPermission("knowledge:list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String status) {
        return Result.success(knowledgeService.page(pageNum, pageSize, keyword, category, status));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("knowledge:list")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(knowledgeService.detail(id));
    }

    @PostMapping("/save")
    @SaCheckPermission("knowledge:edit")
    @Log(title = "知识库维护", businessType = 2)
    public Result<?> save(@RequestBody KnowledgeSaveDTO dto) {
        return Result.success(knowledgeService.save(dto));
    }

    @PostMapping("/draft-from-ticket/{ticketId}")
    @SaCheckPermission("knowledge:edit")
    @Log(title = "工单沉淀知识", businessType = 1)
    public Result<?> draftFromTicket(@PathVariable Long ticketId) {
        return Result.success(knowledgeService.draftFromTicket(ticketId));
    }

    @PostMapping("/publish/{id}")
    @SaCheckPermission("knowledge:publish")
    @Log(title = "知识库发布", businessType = 2)
    public Result<?> publish(@PathVariable Long id) {
        knowledgeService.publish(id);
        return Result.success();
    }

    @PostMapping("/withdraw/{id}")
    @SaCheckPermission("knowledge:publish")
    @Log(title = "知识库下架", businessType = 2)
    public Result<?> withdraw(@PathVariable Long id) {
        knowledgeService.withdraw(id);
        return Result.success();
    }
}
