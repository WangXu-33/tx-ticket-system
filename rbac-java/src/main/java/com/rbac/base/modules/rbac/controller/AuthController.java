package com.rbac.base.modules.rbac.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.rbac.entity.SysMenu;
import com.rbac.base.modules.rbac.entity.SysUser;
import com.rbac.base.modules.rbac.mapper.SysMenuMapper;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import com.rbac.base.modules.rbac.service.PasswordSecurityService;
import com.rbac.base.modules.log.entity.SysLoginLog;
import com.rbac.base.modules.log.mapper.SysLoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysMenuMapper menuMapper;

    @Autowired
    private SysLoginLogMapper loginLogMapper;

    @Autowired
    private PasswordSecurityService passwordSecurityService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 代码生成/修改时间：2026-07-01。
     * 客户注册入口，创建普通客户账号并绑定 customer 角色。
     * 入参：username、password、nickname、phone、email。
     * 出参：新用户 ID。
     * 异常场景：账号重复、密码为空、默认客户角色未初始化时返回错误。
     */
    @PostMapping("/register")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> register(@RequestBody SysUser user) {
        String username = user.getUsername() == null ? null : user.getUsername().trim();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("用户名和密码不能为空");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (exists != null && exists > 0) {
            return Result.error("用户名已存在");
        }

        List<Long> customerRoleIds = jdbcTemplate.queryForList(
                "SELECT id FROM sys_role WHERE role_key = ? AND del_flag = 0 LIMIT 1",
                Long.class,
                "customer"
        );
        if (customerRoleIds.isEmpty()) {
            return Result.error("默认客户角色未初始化");
        }
        Long customerRoleId = customerRoleIds.get(0);

        SysUser registerUser = new SysUser();
        registerUser.setUsername(username);
        registerUser.setPassword(passwordSecurityService.encodePassword(user.getPassword()));
        registerUser.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname().trim() : username);
        registerUser.setPhone(user.getPhone());
        registerUser.setEmail(user.getEmail());
        registerUser.setStatus(1);
        userMapper.insert(registerUser);
        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", registerUser.getId(), customerRoleId);
        return Result.success(registerUser.getId());
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody SysUser user, HttpServletRequest request) {
        // 登录入口只负责身份认证和发 token，角色/权限在 Sa-Token 的 StpInterface 中按需加载。
        String username = user.getUsername() == null ? null : user.getUsername().trim();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(user.getPassword())) {
            return Result.error("用户名和密码不能为空");
        }

        SysUser dbUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUsername(username);
        loginLog.setIpaddr(getClientIp(request));
        loginLog.setLoginTime(LocalDateTime.now());

        if (passwordSecurityService.isLoginLocked(username)) {
            loginLog.setStatus(1);
            loginLog.setMsg(passwordSecurityService.getLoginLockMessage());
            loginLogMapper.insert(loginLog);
            return Result.error(passwordSecurityService.getLoginLockMessage());
        }
        
        if (dbUser == null || !passwordSecurityService.matches(user.getPassword(), dbUser.getPassword())) {
            loginLog.setStatus(1);
            loginLog.setMsg("账号或密码错误");
            loginLogMapper.insert(loginLog);
            return Result.error("账号或密码错误");
        }
        
        if (Integer.valueOf(0).equals(dbUser.getStatus())) {
            loginLog.setStatus(1);
            loginLog.setMsg("账号已被禁用");
            loginLogMapper.insert(loginLog);
            return Result.error("账号已被禁用");
        }

        // 兼容历史明文/旧算法密码：登录成功后顺手升级为当前密码策略。
        passwordSecurityService.upgradeLegacyPasswordIfNeeded(dbUser, user.getPassword());
        StpUtil.login(dbUser.getId());
        
        loginLog.setStatus(0);
        loginLog.setMsg("登录成功");
        loginLogMapper.insert(loginLog);
        
        dbUser.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("user", dbUser);
        return Result.success(data);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @GetMapping("/info")
    public Result<?> getInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        // 这两个调用会进入 StpInterfaceImpl，是前端按钮权限和路由权限的统一来源。
        List<String> permissions = StpUtil.getPermissionList();
        List<String> roles = StpUtil.getRoleList();
        
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("permissions", permissions);
        data.put("roles", roles);
        return Result.success(data);
    }

    @GetMapping("/menus")
    public Result<?> getMenus() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysMenu> menus;
        if (StpUtil.hasRole("admin")) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .orderByAsc(SysMenu::getSort));
        } else {
            // 普通用户只返回角色绑定的菜单，前端动态路由以这里的数据为准。
            menus = menuMapper.getMenusByUserId(userId);
        }
        return Result.success(menus);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        StpUtil.logout();
        return Result.success();
    }
}
