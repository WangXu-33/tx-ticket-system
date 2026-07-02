package com.rbac.base.modules.realtime.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RealtimeAuthService {

    public Long resolveUserId(String token) {
        // Sa-Token 原生支持通过 token 反查 loginId，适合 SSE 这种手动读取请求头的场景。
        if (!StringUtils.hasText(token)) {
            throw notLogin(NotLoginException.NOT_TOKEN);
        }
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            throw notLogin(NotLoginException.INVALID_TOKEN);
        }
        if (loginId instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(loginId.toString());
    }

    private NotLoginException notLogin(String type) {
        return new NotLoginException("未登录，请先登录", "", type);
    }
}
