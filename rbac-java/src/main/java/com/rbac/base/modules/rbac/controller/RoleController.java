package com.rbac.base.modules.rbac.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.rbac.entity.SysRole;
import com.rbac.base.modules.rbac.mapper.SysRoleMapper;
import com.rbac.base.modules.rbac.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/role")
public class RoleController {

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    @GetMapping("/list")
    @SaCheckPermission("sys:role:list")
    public Result<?> list() {
        return Result.success(roleMapper.selectList(null));
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("sys:role:list")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(roleMapper.selectById(id));
    }

    @PostMapping("/add")
    @SaCheckPermission("sys:role:add")
    @Log(title = "角色管理", businessType = 1)
    public Result<?> add(@RequestBody SysRole role) {
        roleMapper.insert(role);
        return Result.success(role.getId());
    }

    @PostMapping("/edit")
    @SaCheckPermission("sys:role:edit")
    @Log(title = "角色管理", businessType = 2)
    public Result<?> edit(@RequestBody SysRole role) {
        roleMapper.updateById(role);
        permissionService.clearPermissionCacheByRoleId(role.getId());
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("sys:role:delete")
    @Log(title = "角色管理", businessType = 3)
    public Result<?> delete(@PathVariable Long id) {
        if (id == 1L) return Result.error("不能删除超级管理员角色");
        permissionService.clearPermissionCacheByRoleId(id);
        roleMapper.deleteById(id);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_role_perm WHERE role_id = ?", id);
        return Result.success();
    }

    @GetMapping("/menuIds/{roleId}")
    @SaCheckPermission("sys:role:edit")
    public Result<?> getMenuIds(@PathVariable Long roleId) {
        List<Long> ids = jdbcTemplate.queryForList("SELECT menu_id FROM sys_role_menu WHERE role_id = ?", Long.class, roleId);
        return Result.success(ids);
    }

    @GetMapping("/deptIds/{roleId}")
    @SaCheckPermission("sys:role:edit")
    public Result<?> getDeptIds(@PathVariable Long roleId) {
        List<Long> ids = jdbcTemplate.queryForList("SELECT dept_id FROM sys_role_dept WHERE role_id = ?", Long.class, roleId);
        return Result.success(ids);
    }

    @PostMapping("/assignMenus")
    @Transactional
    @SaCheckPermission("sys:role:edit")
    @Log(title = "角色授权", businessType = 2)
    public Result<?> assignMenus(@RequestBody Map<String, Object> params) {
        Long roleId = Long.valueOf(params.get("roleId").toString());
        @SuppressWarnings("unchecked")
        List<Number> rawMenuIds = (List<Number>) params.getOrDefault("menuIds", java.util.Collections.emptyList());
        List<Long> menuIds = rawMenuIds.stream().map(Number::longValue).toList();
        
        // 菜单授权决定前端导航可见性；按钮/API 权限由 sys_role_perm 单独控制。
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        for (Long menuId : menuIds) {
            jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
        }
        return Result.success();
    }

    @PostMapping("/assignDepts")
    @Transactional
    @SaCheckPermission("sys:role:edit")
    @Log(title = "角色数据权限", businessType = 2)
    public Result<?> assignDepts(@RequestBody Map<String, Object> params) {
        Long roleId = Long.valueOf(params.get("roleId").toString());
        @SuppressWarnings("unchecked")
        List<Number> rawDeptIds = (List<Number>) params.getOrDefault("deptIds", java.util.Collections.emptyList());
        List<Long> deptIds = rawDeptIds.stream().map(Number::longValue).toList();

        // 只有 data_scope=自定义数据 时，sys_role_dept 才会被 DataScopeAspect 用来过滤部门。
        jdbcTemplate.update("DELETE FROM sys_role_dept WHERE role_id = ?", roleId);
        for (Long deptId : deptIds) {
            jdbcTemplate.update("INSERT INTO sys_role_dept (role_id, dept_id) VALUES (?, ?)", roleId, deptId);
        }
        return Result.success();
    }
}
