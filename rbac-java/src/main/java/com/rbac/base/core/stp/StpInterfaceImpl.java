package com.rbac.base.core.stp;

import cn.dev33.satoken.stp.StpInterface;
import com.rbac.base.modules.rbac.service.PermissionService;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PermissionService permissionService;

    /**
     * Sa-Token 在 @SaCheckPermission 和前端 /auth/info 中都会调用这里。
     * 权限码由“用户 -> 角色 -> 权限”与“用户直授权限”共同计算，admin 角色直接返回通配符。
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        List<String> roles = getRoleList(loginId, loginType);
        if (roles.contains("admin")) {
            return java.util.Collections.singletonList("*");
        }
        // 权限变更后必须清理该缓存，否则在线用户会继续拿到旧权限。
        List<String> cached = permissionService.getCachedPermissionKeys(userId);
        if (cached != null) {
            return cached;
        }
        List<String> permissions = permissionService.getEffectivePermKeysByUserId(userId);
        permissionService.cachePermissionKeys(userId, permissions);
        return permissions;
    }

    /**
     * 角色标识从 sys_role.role_key 加载，是 hasRole("admin") 等判断的依据。
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());
        List<String> cached = permissionService.getCachedRoleKeys(userId);
        if (cached != null) {
            return cached;
        }
        List<String> roles = userMapper.getRolesByUserId(userId);
        permissionService.cacheRoleKeys(userId, roles);
        return roles;
    }
}
