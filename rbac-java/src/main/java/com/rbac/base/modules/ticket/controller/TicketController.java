package com.rbac.base.modules.ticket.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.ticket.dto.TicketAssignDTO;
import com.rbac.base.modules.ticket.dto.TicketCreateDTO;
import com.rbac.base.modules.ticket.dto.TicketReplyDTO;
import com.rbac.base.modules.ticket.dto.TicketResolveDTO;
import com.rbac.base.modules.ticket.service.TicketService;
import org.springframework.web.bind.annotation.*;

/**
 * 代码生成/修改时间：2026-07-01。
 * 工单接口，提供客户提单和运维处理流转能力。
 * 入参：分页参数、工单 DTO、动作 ID。
 * 出参：统一 Result 响应。
 * 异常场景：权限不足、参数缺失、工单不存在时返回错误响应。
 */
@RestController
@RequestMapping("/ticket")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/list")
    @SaCheckPermission(value = {"ticket:list", "ticket:my:list"}, mode = SaMode.OR)
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String priority,
                          @RequestParam(required = false) String owner) {
        return Result.success(ticketService.page(pageNum, pageSize, keyword, status, category, priority, owner));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission(value = {"ticket:list", "ticket:my:list"}, mode = SaMode.OR)
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(ticketService.detail(id));
    }

    @PostMapping("/create")
    @SaCheckPermission(value = {"ticket:add", "ticket:my:add"}, mode = SaMode.OR)
    @Log(title = "工单管理", businessType = 1)
    public Result<?> create(@RequestBody TicketCreateDTO dto) {
        return Result.success(ticketService.create(dto));
    }

    @PostMapping("/receive/{id}")
    @SaCheckPermission("ticket:receive")
    @Log(title = "工单接单", businessType = 2)
    public Result<?> receive(@PathVariable Long id) {
        ticketService.receive(id);
        return Result.success();
    }

    @PostMapping("/reply")
    @SaCheckPermission(value = {"ticket:reply", "ticket:my:reply"}, mode = SaMode.OR)
    @Log(title = "工单回复", businessType = 2)
    public Result<?> reply(@RequestBody TicketReplyDTO dto) {
        ticketService.reply(dto);
        return Result.success();
    }

    @PostMapping("/assign")
    @SaCheckPermission("ticket:assign")
    @Log(title = "工单分派", businessType = 2)
    public Result<?> assign(@RequestBody TicketAssignDTO dto) {
        ticketService.assign(dto);
        return Result.success();
    }

    @PostMapping("/transfer")
    @SaCheckPermission("ticket:transfer")
    @Log(title = "工单转派", businessType = 2)
    public Result<?> transfer(@RequestBody TicketAssignDTO dto) {
        ticketService.transfer(dto);
        return Result.success();
    }

    @PostMapping("/resolve")
    @SaCheckPermission("ticket:resolve")
    @Log(title = "工单解决", businessType = 2)
    public Result<?> resolve(@RequestBody TicketResolveDTO dto) {
        ticketService.resolve(dto);
        return Result.success();
    }

    @PostMapping("/close/{id}")
    @SaCheckPermission(value = {"ticket:close", "ticket:my:close"}, mode = SaMode.OR)
    @Log(title = "工单关闭", businessType = 2)
    public Result<?> close(@PathVariable Long id) {
        ticketService.close(id);
        return Result.success();
    }

    @PostMapping("/reject")
    @SaCheckPermission("ticket:reject")
    @Log(title = "工单驳回", businessType = 2)
    public Result<?> reject(@RequestBody TicketReplyDTO dto) {
        ticketService.reject(dto);
        return Result.success();
    }
}
