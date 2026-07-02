package com.rbac.base.modules.log.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.core.common.report.ExportUtils;
import com.rbac.base.modules.log.entity.SysLoginLog;
import com.rbac.base.modules.log.mapper.SysLoginLogMapper;
import com.rbac.base.modules.log.service.LogReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/system/loginlog")
public class SysLoginLogController {

    @Autowired
    private SysLoginLogMapper loginLogMapper;

    @Autowired
    private LogReportService logReportService;

    @GetMapping("/list")
    @SaCheckPermission("sys:loginlog:list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {

        Page<SysLoginLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            wrapper.like(SysLoginLog::getUsername, username);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysLoginLog::getStatus, status);
        }
        if (StringUtils.hasText(beginTime) && StringUtils.hasText(endTime)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            wrapper.between(SysLoginLog::getLoginTime, LocalDateTime.parse(beginTime, fmt), LocalDateTime.parse(endTime, fmt));
        }

        wrapper.orderByDesc(SysLoginLog::getId);
        return Result.success(loginLogMapper.selectPage(page, wrapper));
    }

    @GetMapping("/stats/summary")
    @SaCheckPermission("sys:loginlog:list")
    public Result<?> summary(@RequestParam(required = false) String username,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate) {
        return Result.success(logReportService.getLoginSummary(username, status, startDate, endDate));
    }

    @GetMapping("/stats/trend")
    @SaCheckPermission("sys:loginlog:list")
    public Result<?> trend(@RequestParam(required = false) String username,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(logReportService.getLoginTrend(username, status, startDate, endDate, days));
    }

    @GetMapping("/export")
    @SaCheckPermission("sys:loginlog:export")
    public void export(@RequestParam(defaultValue = "xlsx") String format,
                       @RequestParam(required = false) String username,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws java.io.IOException {
        var rows = logReportService.listLoginExportRows(username, status, startDate, endDate);
        java.util.List<? extends java.util.List<?>> exportRows = rows.stream()
                .map(item -> (java.util.List<?>) java.util.Arrays.asList(
                        item.getId(),
                        item.getUsername(),
                        item.getIpaddr(),
                        item.getLoginLocation(),
                        item.getBrowser(),
                        item.getOs(),
                        item.getStatus() == 0 ? "成功" : "失败",
                        item.getMsg(),
                        item.getLoginTime()
                ))
                .toList();
        ExportUtils.write(
                format,
                "login-log-report",
                java.util.List.of("编号", "用户账号", "登录IP", "登录地点", "浏览器", "操作系统", "状态", "提示信息", "登录时间"),
                exportRows,
                response
        );
    }

    @DeleteMapping("/delete/{ids}")
    @SaCheckPermission("sys:loginlog:delete")
    @Log(title = "登录日志", businessType = 3)
    public Result<?> delete(@PathVariable Long[] ids) {
        for (Long id : ids) {
            loginLogMapper.deleteById(id);
        }
        return Result.success();
    }
    
    @DeleteMapping("/clean")
    @SaCheckPermission("sys:loginlog:delete")
    @Log(title = "登录日志", businessType = 3)
    public Result<?> clean() {
        loginLogMapper.delete(new LambdaQueryWrapper<>());
        return Result.success();
    }
}
