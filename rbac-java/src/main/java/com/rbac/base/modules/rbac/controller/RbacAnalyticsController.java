package com.rbac.base.modules.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rbac.base.core.common.Result;
import com.rbac.base.core.common.report.ExportUtils;
import com.rbac.base.modules.rbac.service.RbacAnalyticsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping({"/rbac/stats", "/system/rbac/stats"})
public class RbacAnalyticsController {

    private final RbacAnalyticsService analyticsService;

    public RbacAnalyticsController(RbacAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @SaCheckPermission("sys:analytics:list")
    public Result<?> overview() {
        return Result.success(analyticsService.getOverview());
    }

    @GetMapping("/role-distribution")
    @SaCheckPermission("sys:analytics:list")
    public Result<?> roleDistribution() {
        return Result.success(analyticsService.getRoleDistribution());
    }

    @GetMapping("/permission-usage")
    @SaCheckPermission("sys:analytics:list")
    public Result<?> permissionUsage() {
        return Result.success(analyticsService.getPermissionUsage());
    }

    @GetMapping("/user-auth-top")
    @SaCheckPermission("sys:analytics:list")
    public Result<?> userAuthTop() {
        return Result.success(analyticsService.getUserAuthTop());
    }

    @GetMapping("/export")
    @SaCheckPermission("sys:analytics:export")
    public void export(@RequestParam(defaultValue = "xlsx") String format, HttpServletResponse response) throws IOException {
        var overview = analyticsService.getOverview();
        var permissionUsage = analyticsService.getPermissionUsage();
        List<List<?>> rows = new ArrayList<>();
        rows.add(List.of("用户总数", overview.getUserTotal(), "", "", ""));
        rows.add(List.of("角色总数", overview.getRoleTotal(), "", "", ""));
        rows.add(List.of("部门总数", overview.getDeptTotal(), "", "", ""));
        rows.add(List.of("菜单总数", overview.getMenuTotal(), "", "", ""));
        rows.add(List.of("权限总数", overview.getPermissionTotal(), "", "", ""));
        rows.add(List.of("直授权用户数", overview.getDirectGrantUserTotal(), "", "", ""));
        rows.add(List.of("", "", "", "", ""));
        rows.add(List.of("权限标识", "权限名称", "菜单挂载次数", "角色授权次数", "用户直授次数"));
        permissionUsage.forEach(item -> rows.add(List.of(
                item.getPermKey(),
                item.getPermName(),
                item.getMenuBindCount(),
                item.getRoleBindCount(),
                item.getUserBindCount()
        )));
        ExportUtils.write(format, "tx-ticket-analytics-report", List.of("列1", "列2", "列3", "列4", "列5"), rows, response);
    }
}
