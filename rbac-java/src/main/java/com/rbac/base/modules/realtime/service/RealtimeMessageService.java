package com.rbac.base.modules.realtime.service;

import com.rbac.base.modules.realtime.dto.RealtimeConnection;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
public class RealtimeMessageService {

    private final RealtimeConnectionRegistry registry;

    public RealtimeMessageService(RealtimeConnectionRegistry registry) {
        this.registry = registry;
    }

    public void sendAll(String event, Object payload) {
        // 群发：系统公告、全局刷新、服务器探针等不区分用户归属的事件。
        send(registry.findAll(), event, payload);
    }

    public void sendDept(Long deptId, String event, Object payload) {
        // 部门发：当前项目把 deptId 作为“租发”维度，后续可平滑替换为 tenantId/groupId。
        send(registry.findByDeptId(deptId), event, payload);
    }

    public void sendUser(Long userId, String event, Object payload) {
        // 单发：同一用户可能多端登录，因此 userId 会对应多个 SSE 连接。
        send(registry.findByUserId(userId), event, payload);
    }

    private void send(List<RealtimeConnection> connections, String event, Object payload) {
        for (RealtimeConnection connection : connections) {
            try {
                connection.emitter().send(SseEmitter.event()
                        .id(connection.emitterId())
                        .name(event)
                        .data(payload));
            } catch (IOException | IllegalStateException ex) {
                // send 失败说明连接已不可用，立即清理，避免后续定时任务持续打到坏连接。
                registry.remove(connection.emitterId());
            }
        }
    }
}
