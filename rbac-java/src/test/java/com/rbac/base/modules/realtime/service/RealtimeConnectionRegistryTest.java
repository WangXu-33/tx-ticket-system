package com.rbac.base.modules.realtime.service;

import com.rbac.base.modules.realtime.dto.RealtimeConnection;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealtimeConnectionRegistryTest {

    @Test
    void registerIndexesConnectionsByUserAndDepartment() {
        RealtimeConnectionRegistry registry = new RealtimeConnectionRegistry();
        RealtimeConnection userOneFirst = connection("c1", 1L, 10L);
        RealtimeConnection userOneSecond = connection("c2", 1L, 10L);
        RealtimeConnection userTwo = connection("c3", 2L, 20L);

        registry.register(userOneFirst);
        registry.register(userOneSecond);
        registry.register(userTwo);

        assertEquals(3, registry.findAll().size());
        assertEquals(2, registry.findByUserId(1L).size());
        assertEquals(2, registry.findByDeptId(10L).size());
        assertEquals(List.of(userTwo), registry.findByUserId(2L));
    }

    @Test
    void removeDeletesConnectionFromAllIndexes() {
        RealtimeConnectionRegistry registry = new RealtimeConnectionRegistry();
        RealtimeConnection connection = connection("c1", 1L, 10L);
        registry.register(connection);

        registry.remove("c1");

        assertTrue(registry.findAll().isEmpty());
        assertTrue(registry.findByUserId(1L).isEmpty());
        assertTrue(registry.findByDeptId(10L).isEmpty());
    }

    @Test
    void removeMissingConnectionDoesNothing() {
        RealtimeConnectionRegistry registry = new RealtimeConnectionRegistry();
        RealtimeConnection connection = connection("c1", 1L, 10L);
        registry.register(connection);

        registry.remove("missing");

        assertEquals(List.of(connection), registry.findAll());
    }

    private RealtimeConnection connection(String emitterId, Long userId, Long deptId) {
        return new RealtimeConnection(
                emitterId,
                userId,
                deptId,
                LocalDateTime.now(),
                LocalDateTime.now(),
                new SseEmitter(0L)
        );
    }
}
