package com.rbac.base.modules.rbac.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rbac.base.modules.rbac.entity.SysPermission;
import com.rbac.base.modules.rbac.mapper.SysPermissionMapper;
import com.rbac.base.modules.rbac.vo.PermissionGrantVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
public class PermissionService {

    public static final String PERMISSION_CACHE_KEY = "permissionKeys";
    public static final String ROLE_CACHE_KEY = "roleKeys";

    @Autowired
    private SysPermissionMapper permissionMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getEffectivePermKeysByUserId(Long userId) {
        // 有效权限的最终口径在 Mapper SQL 中维护，避免 Controller 分散拼装权限来源。
        return permissionMapper.selectEffectivePermKeysByUserId(userId);
    }

    public List<SysPermission> listByMenuId(Long menuId) {
        return permissionMapper.selectByMenuId(menuId);
    }

    public Page<PermissionGrantVO> pagePermissions(int pageNum, int pageSize, String keyword, Long menuId) {
        return permissionMapper.selectPermissionGrantPage(
                new Page<>(pageNum, pageSize),
                StringUtils.hasText(keyword) ? keyword.trim() : null,
                menuId
        );
    }

    public List<Long> getRolePermIds(Long roleId) {
        return permissionMapper.selectRolePermIds(roleId);
    }

    public List<Long> getUserPermIds(Long userId) {
        return jdbcTemplate.queryForList("SELECT perm_id FROM sys_user_perm WHERE user_id = ? ORDER BY perm_id", Long.class, userId);
    }

    @Transactional
    public SysPermission attachPermissionToMenu(Long menuId, String name, String permKey) {
        if (menuId == null || !StringUtils.hasText(name) || !StringUtils.hasText(permKey)) {
            throw new IllegalArgumentException("菜单与权限信息不能为空");
        }

        // perm_key 全局唯一；菜单只是挂载权限入口，同一个权限码可以被复用到不同菜单展示。
        SysPermission permission = permissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getPermKey, permKey)
                .last("limit 1"));

        if (permission == null) {
            permission = new SysPermission();
            permission.setName(name.trim());
            permission.setPermKey(permKey.trim());
            permission.setStatus(1);
            permissionMapper.insert(permission);
        } else if (!name.trim().equals(permission.getName())) {
            permission.setName(name.trim());
            permissionMapper.updateById(permission);
        }

        jdbcTemplate.update("INSERT IGNORE INTO sys_menu_permission(menu_id, perm_id) VALUES (?, ?)", menuId, permission.getId());
        return permission;
    }

    @Transactional
    public void detachPermissionFromMenu(Long menuId, Long permId) {
        jdbcTemplate.update("DELETE FROM sys_menu_permission WHERE menu_id = ? AND perm_id = ?", menuId, permId);
    }

    @Transactional
    public void assignRolePermissions(Long roleId, List<Long> permIds) {
        // 角色授权采用“先删后插”的全量覆盖模式，前端提交的是角色当前完整权限集合。
        jdbcTemplate.update("DELETE FROM sys_role_perm WHERE role_id = ?", roleId);
        if (permIds != null && !permIds.isEmpty()) {
            jdbcTemplate.batchUpdate("INSERT INTO sys_role_perm(role_id, perm_id) VALUES (?, ?)", permIds, permIds.size(),
                    (ps, permId) -> {
                        ps.setLong(1, roleId);
                        ps.setLong(2, permId);
                    });
        }
        // 角色权限变更会影响该角色下所有在线用户，必须逐个清理会话缓存。
        clearPermissionCacheByRoleId(roleId);
    }

    @Transactional
    public void assignUserPermissions(Long userId, List<Long> permIds) {
        // 用户直授权限用于补充角色权限，适合临时例外授权；撤销同样走全量覆盖。
        jdbcTemplate.update("DELETE FROM sys_user_perm WHERE user_id = ?", userId);
        if (permIds != null && !permIds.isEmpty()) {
            jdbcTemplate.batchUpdate("INSERT INTO sys_user_perm(user_id, perm_id) VALUES (?, ?)", permIds, permIds.size(),
                    (ps, permId) -> {
                        ps.setLong(1, userId);
                        ps.setLong(2, permId);
                    });
        }
        clearPermissionCacheByUserId(userId);
    }

    public void clearPermissionCacheByUserId(Long userId) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(userId, false);
            if (session != null) {
                // 权限和角色都缓存于登录会话，授权关系变化后要同时失效。
                session.delete(PERMISSION_CACHE_KEY);
                session.delete(ROLE_CACHE_KEY);
            }
        } catch (Exception ignored) {
        }
    }

    public void clearPermissionCacheByRoleId(Long roleId) {
        // 反查拥有该角色的用户，保证角色授权修改对在线会话立即生效。
        List<Long> userIds = jdbcTemplate.queryForList("SELECT user_id FROM sys_user_role WHERE role_id = ?", Long.class, roleId);
        for (Long userId : userIds) {
            clearPermissionCacheByUserId(userId);
        }
    }

    public List<String> getCachedPermissionKeys(Long userId) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(userId, false);
            if (session == null) {
                return null;
            }
            Object cached = session.get(PERMISSION_CACHE_KEY);
            if (cached instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) cached;
                return list;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public List<String> getCachedRoleKeys(Long userId) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(userId, false);
            if (session == null) {
                return null;
            }
            Object cached = session.get(ROLE_CACHE_KEY);
            if (cached instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) cached;
                return list;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void cachePermissionKeys(Long userId, List<String> permissionKeys) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(userId);
            session.set(PERMISSION_CACHE_KEY, permissionKeys == null ? Collections.emptyList() : permissionKeys);
        } catch (Exception ignored) {
        }
    }

    public void cacheRoleKeys(Long userId, List<String> roleKeys) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(userId);
            session.set(ROLE_CACHE_KEY, roleKeys == null ? Collections.emptyList() : roleKeys);
        } catch (Exception ignored) {
        }
    }
}
