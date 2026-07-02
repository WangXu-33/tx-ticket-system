package com.rbac.base.modules.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.rbac.service.PermissionService;
import com.rbac.base.modules.rbac.vo.PermissionGrantVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/permission")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/list")
    @SaCheckPermission("sys:menu:list")
    public Result<?> list(@RequestParam Long menuId) {
        return Result.success(permissionService.listByMenuId(menuId));
    }

    @GetMapping("/page")
    @SaCheckPermission("sys:role:edit")
    public Result<?> page(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long menuId) {
        Page<PermissionGrantVO> page = permissionService.pagePermissions(pageNum, pageSize, keyword, menuId);
        return Result.success(page);
    }

    @PostMapping("/add")
    @SaCheckPermission("sys:menu:edit")
    public Result<?> add(@RequestBody Map<String, Object> params) {
        Long menuId = Long.valueOf(params.get("menuId").toString());
        String name = params.get("name").toString();
        String permKey = params.get("permKey").toString();
        // 这里维护“菜单 -> 权限码”的展示关系，真正授权仍由角色/用户绑定权限完成。
        return Result.success(permissionService.attachPermissionToMenu(menuId, name, permKey));
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("sys:menu:edit")
    public Result<?> delete(@PathVariable Long id, @RequestParam Long menuId) {
        permissionService.detachPermissionFromMenu(menuId, id);
        return Result.success();
    }

    @GetMapping("/role/{roleId}/ids")
    @SaCheckPermission("sys:role:edit")
    public Result<?> rolePermissionIds(@PathVariable Long roleId) {
        List<Long> permIds = permissionService.getRolePermIds(roleId);
        return Result.success(permIds);
    }

    @PostMapping("/role/assign")
    @SaCheckPermission("sys:role:edit")
    public Result<?> assignRolePermissions(@RequestBody Map<String, Object> params) {
        Long roleId = Long.valueOf(params.get("roleId").toString());
        @SuppressWarnings("unchecked")
        List<Number> rawPermIds = (List<Number>) params.getOrDefault("permIds", java.util.Collections.emptyList());
        List<Long> permIds = rawPermIds.stream().map(Number::longValue).toList();
        // 角色绑定权限码后，@SaCheckPermission 才能在接口层做按钮/API 级校验。
        permissionService.assignRolePermissions(roleId, permIds);
        return Result.success();
    }
}
