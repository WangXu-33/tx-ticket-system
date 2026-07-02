package com.rbac.base.modules.log.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.core.common.report.ExportUtils;
import com.rbac.base.modules.log.entity.SysOperLog;
import com.rbac.base.modules.log.mapper.SysOperLogMapper;
import com.rbac.base.modules.log.service.LogReportService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/system/operlog")
public class SysOperLogController {

    @Autowired
    private SysOperLogMapper operLogMapper;

    @Autowired
    private LogReportService logReportService;

    @GetMapping("/list")
    @SaCheckPermission("sys:operlog:list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String operName,
            @RequestParam(required = false) Integer businessType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {

        Page<SysOperLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(title)) {
            wrapper.like(SysOperLog::getTitle, title);
        }
        if (StringUtils.hasText(operName)) {
            wrapper.like(SysOperLog::getOperName, operName);
        }
        if (businessType != null) {
            wrapper.eq(SysOperLog::getBusinessType, businessType);
        }
        if (status != null) {
            wrapper.eq(SysOperLog::getStatus, status);
        }
        if (StringUtils.hasText(beginTime) && StringUtils.hasText(endTime)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            wrapper.between(SysOperLog::getOperTime, LocalDateTime.parse(beginTime, fmt), LocalDateTime.parse(endTime, fmt));
        }
        
        wrapper.orderByDesc(SysOperLog::getId);
        return Result.success(operLogMapper.selectPage(page, wrapper));
    }

    @GetMapping("/stats/summary")
    @SaCheckPermission("sys:operlog:list")
    public Result<?> summary(@RequestParam(required = false) String title,
                             @RequestParam(required = false) String operName,
                             @RequestParam(required = false) Integer businessType,
                             @RequestParam(required = false) Integer status,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate) {
        return Result.success(logReportService.getOperSummary(title, operName, businessType, status, startDate, endDate));
    }

    @GetMapping("/stats/trend")
    @SaCheckPermission("sys:operlog:list")
    public Result<?> trend(@RequestParam(required = false) String title,
                           @RequestParam(required = false) String operName,
                           @RequestParam(required = false) Integer businessType,
                           @RequestParam(required = false) Integer status,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(logReportService.getOperTrend(title, operName, businessType, status, startDate, endDate, days));
    }

    @GetMapping("/stats/top-modules")
    @SaCheckPermission("sys:operlog:list")
    public Result<?> topModules(@RequestParam(required = false) String title,
                                @RequestParam(required = false) String operName,
                                @RequestParam(required = false) Integer businessType,
                                @RequestParam(required = false) Integer status,
                                @RequestParam(required = false) String startDate,
                                @RequestParam(required = false) String endDate) {
        return Result.success(logReportService.getOperTopModules(title, operName, businessType, status, startDate, endDate));
    }

    @GetMapping("/export")
    @SaCheckPermission("sys:operlog:export")
    public void export(@RequestParam(defaultValue = "xlsx") String format,
                       @RequestParam(required = false) String title,
                       @RequestParam(required = false) String operName,
                       @RequestParam(required = false) Integer businessType,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws java.io.IOException {
        var rows = logReportService.listOperExportRows(title, operName, businessType, status, startDate, endDate);
        java.util.List<? extends java.util.List<?>> exportRows = rows.stream()
                .map(item -> (java.util.List<?>) java.util.Arrays.asList(
                        item.getId(),
                        item.getTitle(),
                        item.getBusinessType(),
                        item.getOperName(),
                        item.getRequestMethod(),
                        item.getOperUrl(),
                        item.getOperIp(),
                        item.getStatus() == 0 ? "成功" : "失败",
                        item.getCostTime(),
                        item.getOperTime()
                ))
                .toList();
        ExportUtils.write(
                format,
                "oper-log-report",
                java.util.List.of("编号", "模块", "操作类型", "操作人员", "请求方式", "请求地址", "主机IP", "状态", "耗时(ms)", "操作时间"),
                exportRows,
                response
        );
    }

    @DeleteMapping("/delete/{ids}")
    @SaCheckPermission("sys:operlog:delete")
    @Log(title = "操作日志", businessType = 3)
    public Result<?> delete(@PathVariable Long[] ids) {
        for (Long id : ids) {
            operLogMapper.deleteById(id);
        }
        return Result.success();
    }
    
    @DeleteMapping("/clean")
    @SaCheckPermission("sys:operlog:delete")
    @Log(title = "操作日志", businessType = 3)
    public Result<?> clean() {
        operLogMapper.delete(new LambdaQueryWrapper<>());
        return Result.success();
    }
}
