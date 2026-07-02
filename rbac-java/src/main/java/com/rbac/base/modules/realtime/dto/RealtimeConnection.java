package com.rbac.base.modules.realtime.dto;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;

public record RealtimeConnection(
        String emitterId,
        Long userId,
        Long deptId,
        LocalDateTime createdAt,
        LocalDateTime lastActiveAt,
        SseEmitter emitter
) {
}
