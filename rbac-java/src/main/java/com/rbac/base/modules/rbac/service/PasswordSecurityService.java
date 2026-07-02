package com.rbac.base.modules.rbac.service;

import cn.hutool.crypto.digest.BCrypt;
import com.rbac.base.modules.rbac.entity.SysUser;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
public class PasswordSecurityService {

    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int LOGIN_FAIL_LIMIT = 5;
    private static final int LOGIN_LOCK_MINUTES = 15;
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$.{56}$");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SysUserMapper userMapper;

    public void validatePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        String value = rawPassword.trim();
        if (value.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("密码长度不能少于6位");
        }
        if (value.length() > 32) {
            throw new IllegalArgumentException("密码长度不能超过32位");
        }
    }

    public String encodePassword(String rawPassword) {
        validatePassword(rawPassword);
        return BCrypt.hashpw(rawPassword.trim(), BCrypt.gensalt());
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (isEncodedPassword(storedPassword)) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }
        return storedPassword.equals(rawPassword);
    }

    public boolean shouldUpgradePassword(String storedPassword) {
        return StringUtils.hasText(storedPassword) && !isEncodedPassword(storedPassword);
    }

    public boolean isLoginLocked(String username) {
        Long failedCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(1)
                FROM sys_login_log
                WHERE username = ?
                  AND status = 1
                  AND msg = '账号或密码错误'
                  AND login_time >= DATE_SUB(NOW(), INTERVAL ? MINUTE)
                """,
                Long.class,
                username,
                LOGIN_LOCK_MINUTES
        );
        return failedCount != null && failedCount >= LOGIN_FAIL_LIMIT;
    }

    public String getLoginLockMessage() {
        return "连续登录失败次数过多，请15分钟后重试";
    }

    public void upgradeLegacyPasswordIfNeeded(SysUser user, String rawPassword) {
        if (user == null || user.getId() == null || !shouldUpgradePassword(user.getPassword())) {
            return;
        }
        SysUser updateObj = new SysUser();
        updateObj.setId(user.getId());
        updateObj.setPassword(encodePassword(rawPassword));
        userMapper.updateById(updateObj);
        user.setPassword(updateObj.getPassword());
    }

    public void resetPassword(Long userId, String newPassword) {
        SysUser updateObj = new SysUser();
        updateObj.setId(userId);
        updateObj.setPassword(encodePassword(newPassword));
        userMapper.updateById(updateObj);
    }

    private boolean isEncodedPassword(String storedPassword) {
        return BCRYPT_PATTERN.matcher(storedPassword).matches();
    }
}
