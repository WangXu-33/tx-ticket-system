package com.rbac.base.modules.realtime.controller;

import cn.dev33.satoken.exception.NotLoginException;
import com.rbac.base.modules.rbac.entity.SysUser;
import com.rbac.base.modules.rbac.mapper.SysUserMapper;
import com.rbac.base.modules.realtime.dto.RealtimeConnection;
import com.rbac.base.modules.realtime.service.RealtimeAuthService;
import com.rbac.base.modules.realtime.service.RealtimeConnectionRegistry;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/realtime")
public class RealtimeSseController {

    private static final long SSE_TIMEOUT = 0L;

    private final RealtimeAuthService authService;
    private final SysUserMapper userMapper;
    private final RealtimeConnectionRegistry registry;

    public RealtimeSseController(RealtimeAuthService authService,
                                 SysUserMapper userMapper,
                                 RealtimeConnectionRegistry registry) {
        this.authService = authService;
        this.userMapper = userMapper;
        this.registry = registry;
    }

    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@RequestHeader(value = "Authorization", required = false) String token) {
        // SSE 使用 fetch 建连，token 仍走 Authorization 请求头；服务端绝不信任前端传入 userId/deptId。
        if (!StringUtils.hasText(token)) {
            throw new NotLoginException("未登录，请先登录", "", NotLoginException.NOT_TOKEN);
        }

        Long userId = authService.resolveUserId(token);
        SysUser user = userMapper.selectById(userId);
        Long deptId = user == null ? null : user.getDeptId();
        // deptId 是当前“租发”的分组依据，后续用户组/角色组可在连接上下文中继续扩展索引。
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        LocalDateTime now = LocalDateTime.now();
        RealtimeConnection connection = new RealtimeConnection(emitterId, userId, deptId, now, now, emitter);

        // 浏览器刷新、断网、超时或服务端发送失败时，都必须从所有索引中移除连接。
        emitter.onCompletion(() -> registry.remove(emitterId));
        emitter.onTimeout(() -> registry.remove(emitterId));
        emitter.onError(throwable -> registry.remove(emitterId));
        registry.register(connection);
        sendConnectedEvent(emitter, emitterId);
        return emitter;
    }

    private void sendConnectedEvent(SseEmitter emitter, String emitterId) {
        try {
            // 首包用于让前端尽快进入“已连接”状态，也能验证代理没有吞掉 event-stream。
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("connected")
                    .data(Map.of("status", "connected")));
        } catch (IOException | IllegalStateException ex) {
            emitter.completeWithError(ex);
        }
    }
}
