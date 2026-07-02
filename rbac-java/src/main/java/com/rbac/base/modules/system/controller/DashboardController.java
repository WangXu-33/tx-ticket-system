package com.rbac.base.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.system.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    @SaCheckPermission("sys:dashboard:list")
    public Result<?> overview() {
        return Result.success(dashboardService.getOverview());
    }

    @GetMapping("/trend")
    @SaCheckPermission("sys:dashboard:list")
    public Result<?> trend(Integer days) {
        return Result.success(dashboardService.getTrend(days));
    }

    @GetMapping("/distribution")
    @SaCheckPermission("sys:dashboard:list")
    public Result<?> distribution() {
        return Result.success(dashboardService.getDistribution());
    }

    @GetMapping("/server")
    @SaCheckPermission("sys:dashboard:list")
    public Result<?> server() {
        return Result.success(dashboardService.getServerMetrics());
    }

    @GetMapping("/stats")
    @SaCheckPermission("sys:dashboard:list")
    public Result<?> getStats() {
        var overview = dashboardService.getOverview();
        var server = dashboardService.getServerMetrics();
        java.util.Map<String, Object> stats = new java.util.LinkedHashMap<>();
        stats.put("userCount", overview.getUserTotal());
        stats.put("logCount", overview.getTodayOperTotal());
        stats.put("fileCount", overview.getFileTotal());
        stats.put("deptCount", overview.getDeptTotal());
        java.util.Map<String, Object> serverMap = new java.util.LinkedHashMap<>();
        serverMap.put("cpuUsage", server.getCpuUsage());
        serverMap.put("memUsage", server.getMemoryUsage());
        serverMap.put("totalMem", server.getJvmMax());
        serverMap.put("usedMem", server.getJvmUsed());
        stats.put("server", serverMap);
        return Result.success(stats);
    }
}
