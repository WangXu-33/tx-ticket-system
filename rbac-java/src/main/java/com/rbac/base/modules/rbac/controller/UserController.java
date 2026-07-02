package com.rbac.base.modules.rbac.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.core.annotation.DataScope;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.rbac.entity.SysUser;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import com.rbac.base.modules.rbac.service.PasswordSecurityService;
import com.rbac.base.modules.rbac.service.PermissionService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class UserController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PasswordSecurityService passwordSecurityService;

    @GetMapping("/list")
    @SaCheckPermission("sys:user:list")
    @DataScope(deptAlias = "", userAlias = "", userColumn = "id")
    public Result<?> list(@RequestParam(required = false) String username) {
        // 用户列表是数据权限的典型入口：@DataScope 会根据当前登录人的角色范围追加过滤条件。
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .like(StringUtils.hasText(username), SysUser::getUsername, username)
                .orderByDesc(SysUser::getId));
        users.forEach(item -> item.setPassword(null));
        return Result.success(users);
    }

    @GetMapping("/detail/{id}")
    @SaCheckPermission("sys:user:list")
    public Result<?> detail(@PathVariable Long id) {
        SysUser user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null); // 防止密码泄露
        }
        return Result.success(user);
    }

    @GetMapping("/profile")
    public Result<?> profile() {
        Long currentUserId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(currentUserId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    @PostMapping("/add")
    @SaCheckPermission("sys:user:add")
    @Log(title = "用户管理", businessType = 1)
    public Result<?> add(@RequestBody SysUser user) {
        user.setPassword(passwordSecurityService.encodePassword(user.getPassword()));
        userMapper.insert(user);
        return Result.success(user.getId());
    }

    @PostMapping("/edit")
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户管理", businessType = 2)
    public Result<?> edit(@RequestBody SysUser user) {
        if (!StringUtils.hasText(user.getPassword())) {
            user.setPassword(null);
        } else {
            user.setPassword(passwordSecurityService.encodePassword(user.getPassword()));
        }
        userMapper.updateById(user);
        permissionService.clearPermissionCacheByUserId(user.getId());
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("sys:user:delete")
    @Log(title = "用户管理", businessType = 3)
    public Result<?> delete(@PathVariable Long id) {
        if (id == 1L) return Result.error("不能删除超级管理员");
        permissionService.clearPermissionCacheByUserId(id);
        StpUtil.kickout(id);
        userMapper.deleteById(id);
        // 删除关联角色
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", id);
        jdbcTemplate.update("DELETE FROM sys_user_perm WHERE user_id = ?", id);
        return Result.success();
    }

    // --- 角色分配 ---

    @GetMapping("/roles/{userId}")
    @SaCheckPermission("sys:user:edit")
    public Result<?> getUserRoles(@PathVariable Long userId) {
        List<Long> roleIds = jdbcTemplate.queryForList("SELECT role_id FROM sys_user_role WHERE user_id = ?", Long.class, userId);
        return Result.success(roleIds);
    }

    @GetMapping("/perms/{userId}")
    @SaCheckPermission("sys:user:assignPerm")
    public Result<?> getUserPermIds(@PathVariable Long userId) {
        return Result.success(permissionService.getUserPermIds(userId));
    }

    @PostMapping("/assignRoles")
    @Transactional
    @SaCheckPermission("sys:user:edit")
    @Log(title = "用户分配角色", businessType = 2)
    public Result<?> assignRoles(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        @SuppressWarnings("unchecked")
        List<Number> rawRoleIds = (List<Number>) params.getOrDefault("roleIds", java.util.Collections.emptyList());
        List<Long> roleIds = rawRoleIds.stream().map(Number::longValue).toList();
        
        if (userId == 1L && !roleIds.contains(1L)) {
            return Result.error("不能取消超级管理员的管理员角色");
        }

        // 用户角色采用全量覆盖，提交前端勾选结果即为最终角色集合。
        jdbcTemplate.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        for (Long roleId : roleIds) {
            jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", userId, roleId);
        }
        // 角色变化会改变菜单、按钮权限和数据范围，立即清理登录会话缓存。
        permissionService.clearPermissionCacheByUserId(userId);
        return Result.success();
    }

    @PostMapping("/assignPerms")
    @Transactional
    @SaCheckPermission("sys:user:assignPerm")
    @Log(title = "用户直授权限", businessType = 2)
    public Result<?> assignPerms(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        @SuppressWarnings("unchecked")
        List<Number> rawPermIds = (List<Number>) params.getOrDefault("permIds", java.util.Collections.emptyList());
        List<Long> permIds = rawPermIds.stream().map(Number::longValue).toList();
        // 用户直授权限是角色权限之外的补充授权，适合少量例外，不建议替代角色模型。
        permissionService.assignUserPermissions(userId, permIds);
        return Result.success();
    }

    @PostMapping("/updateProfile")
    @Log(title = "个人中心", businessType = 2)
    public Result<?> updateProfile(@RequestBody ProfileUpdateDTO profile) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        SysUser updateObj = new SysUser();
        updateObj.setId(currentUserId);
        updateObj.setNickname(profile.getNickname());
        updateObj.setAvatarFileId(profile.getAvatarFileId());
        updateObj.setEmail(profile.getEmail());
        updateObj.setPhone(profile.getPhone());
        updateObj.setSex(profile.getSex());
        updateObj.setBirthDate(profile.getBirthDate());
        updateObj.setAddress(profile.getAddress());
        userMapper.updateById(updateObj);
        return Result.success();
    }

    @PostMapping("/updatePwd")
    @Log(title = "个人中心", businessType = 2)
    public Result<?> updatePwd(@RequestBody Map<String, String> params) {
        String oldPwd = params.get("oldPwd");
        String newPwd = params.get("newPwd");
        Long userId = StpUtil.getLoginIdAsLong();
        
        SysUser user = userMapper.selectById(userId);
        if (!passwordSecurityService.matches(oldPwd, user.getPassword())) {
            return Result.error("旧密码错误");
        }
        
        SysUser updateObj = new SysUser();
        updateObj.setId(userId);
        updateObj.setPassword(passwordSecurityService.encodePassword(newPwd));
        userMapper.updateById(updateObj);
        StpUtil.kickout(userId);
        return Result.success();
    }

    @PostMapping("/resetPwd")
    @SaCheckPermission("sys:user:resetPwd")
    @Log(title = "重置用户密码", businessType = 2)
    public Result<?> resetPwd(@RequestBody Map<String, String> params) {
        if (!StringUtils.hasText(params.get("userId"))) {
            return Result.error("用户不能为空");
        }
        Long userId = Long.valueOf(params.get("userId"));
        String newPassword = params.get("newPassword");
        passwordSecurityService.resetPassword(userId, newPassword);
        StpUtil.kickout(userId);
        return Result.success();
    }

    @PostMapping("/kickout/{id}")
    @SaCheckPermission("sys:user:kickout")
    @Log(title = "强制用户下线", businessType = 2)
    public Result<?> kickout(@PathVariable Long id) {
        StpUtil.kickout(id);
        permissionService.clearPermissionCacheByUserId(id);
        return Result.success();
    }

    @Data
    public static class ProfileUpdateDTO {
        private String nickname;
        private Long avatarFileId;
        private String email;
        private String phone;
        private Integer sex;
        private LocalDate birthDate;
        private String address;
    }
}
