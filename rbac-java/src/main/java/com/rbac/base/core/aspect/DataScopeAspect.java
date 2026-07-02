package com.rbac.base.core.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.rbac.base.core.annotation.DataScope;
import com.rbac.base.core.config.DataScopeContextHolder;
import com.rbac.base.modules.rbac.entity.SysDept;
import com.rbac.base.modules.rbac.entity.SysRole;
import com.rbac.base.modules.rbac.entity.SysUser;
import com.rbac.base.modules.rbac.mapper.SysDeptMapper;
import com.rbac.base.modules.rbac.mapper.SysRoleMapper;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
public class DataScopeAspect {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 数据权限常量
    public static final int DATA_SCOPE_ALL = 1;         // 全部数据
    public static final int DATA_SCOPE_CUSTOM = 2;      // 自定义数据
    public static final int DATA_SCOPE_DEPT = 3;        // 本部门数据
    public static final int DATA_SCOPE_DEPT_AND_CHILD = 4; // 本部门及以下数据
    public static final int DATA_SCOPE_SELF = 5;        // 仅本人数据

    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint joinPoint, DataScope dataScope) {
        DataScopeContextHolder.clear(); // 先清理上下文，防脏数据

        if (!StpUtil.isLogin()) {
            return;
        }

        long userId = StpUtil.getLoginIdAsLong();
        // 超级管理员(例如ID为1)不限制
        if (userId == 1L) {
            return;
        }

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        // 一个用户可拥有多个角色，数据范围取“各角色可见范围的并集”。
        List<Long> roleIds = jdbcTemplate.queryForList("SELECT role_id FROM sys_user_role WHERE user_id = ?", Long.class, userId);
        if (roleIds.isEmpty()) {
            // 没有角色，只能看自己
            injectSql(dataScope, user, DATA_SCOPE_SELF);
            return;
        }

        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);
        if (roles.isEmpty()) {
            return;
        }

        StringBuilder sqlString = new StringBuilder();
        boolean isFirst = true;

        for (SysRole role : roles) {
            Integer scope = role.getDataScope();
            if (scope == null) continue;

            if (scope == DATA_SCOPE_ALL) {
                return; // 如果拥有全部数据权限，则不注入任何限制条件
            }

            // 每个角色独立生成一段 SQL，最后用 OR 合并，避免高权限角色被低权限角色收窄。
            String roleSql = getRoleDataScopeSql(dataScope, user, scope, role.getId());
            if (StringUtils.hasText(roleSql)) {
                if (!isFirst) {
                    sqlString.append(" OR ");
                } else {
                    isFirst = false;
                }
                sqlString.append("(").append(roleSql).append(")");
            }
        }

        if (StringUtils.hasText(sqlString.toString())) {
            DataScopeContextHolder.setSqlFilter(sqlString.toString());
        } else {
            injectSql(dataScope, user, DATA_SCOPE_SELF);
        }
    }

    @org.aspectj.lang.annotation.After("@annotation(dataScope)")
    public void doAfter(JoinPoint joinPoint, DataScope dataScope) {
        DataScopeContextHolder.clear(); // 方法执行完，彻底清理上下文，防止内存泄漏
    }

    private String getRoleDataScopeSql(DataScope dataScope, SysUser user, int scope, Long roleId) {
        // 注解提供表别名和字段名，确保同一套数据范围逻辑可复用到不同查询 SQL。
        String deptAlias = dataScope.deptAlias();
        String userAlias = dataScope.userAlias();
        String deptColumn = dataScope.deptColumn();
        String userColumn = dataScope.userColumn();
        
        String dAlias = StringUtils.hasText(deptAlias) ? deptAlias + "." : "";
        String uAlias = StringUtils.hasText(userAlias) ? userAlias + "." : "";
        String deptField = dAlias + deptColumn;
        String userField = uAlias + userColumn;

        Long deptId = user.getDeptId();
        
        switch (scope) {
            case DATA_SCOPE_CUSTOM:
                // 自定义数据范围使用 sys_role_dept 维护角色可访问的部门集合。
                return deptField + " IN (SELECT dept_id FROM sys_role_dept WHERE role_id = " + roleId + ")";
            case DATA_SCOPE_DEPT:
                if (deptId == null) return "1=0";
                return deptField + " = " + deptId;
            case DATA_SCOPE_DEPT_AND_CHILD:
                if (deptId == null) return "1=0";
                SysDept dept = deptMapper.selectById(deptId);
                if (dept == null || !StringUtils.hasText(dept.getDeptCode())) return "1=0";
                // dept_code 前缀代表部门树路径，因此 LIKE 前缀可覆盖本部门及下级部门。
                return deptField + " IN (SELECT id FROM sys_dept WHERE dept_code LIKE '" + dept.getDeptCode() + "%')";
            case DATA_SCOPE_SELF:
                // 仅本人数据通常作用于 create_by，也可通过注解改成业务表的用户字段。
                return userField + " = " + user.getId();
            default:
                return "1=0";
        }
    }

    private void injectSql(DataScope dataScope, SysUser user, int scope) {
        String sql = getRoleDataScopeSql(dataScope, user, scope, null);
        if (StringUtils.hasText(sql)) {
            DataScopeContextHolder.setSqlFilter(sql);
        }
    }
}
