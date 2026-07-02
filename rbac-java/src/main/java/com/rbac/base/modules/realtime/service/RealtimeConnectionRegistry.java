package com.rbac.base.modules.realtime.service;

import com.rbac.base.modules.realtime.dto.RealtimeConnection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RealtimeConnectionRegistry {

    // 三套索引指向同一批连接：全量广播走 connections，单发/部门发走反向索引。
    private final ConcurrentMap<String, RealtimeConnection> connections = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Set<String>> userIndex = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Set<String>> deptIndex = new ConcurrentHashMap<>();

    public void register(RealtimeConnection connection) {
        connections.put(connection.emitterId(), connection);
        index(userIndex, connection.userId(), connection.emitterId());
        index(deptIndex, connection.deptId(), connection.emitterId());
    }

    public void remove(String emitterId) {
        RealtimeConnection removed = connections.remove(emitterId);
        if (removed == null) {
            return;
        }
        // 删除主连接后必须同步清理反向索引，否则后续部门/用户推送会命中失效 emitterId。
        unindex(userIndex, removed.userId(), emitterId);
        unindex(deptIndex, removed.deptId(), emitterId);
    }

    public List<RealtimeConnection> findAll() {
        return new ArrayList<>(connections.values());
    }

    public List<RealtimeConnection> findByUserId(Long userId) {
        return findByIndex(userIndex, userId);
    }

    public List<RealtimeConnection> findByDeptId(Long deptId) {
        return findByIndex(deptIndex, deptId);
    }

    private void index(ConcurrentMap<Long, Set<String>> index, Long key, String emitterId) {
        if (key == null) {
            return;
        }
        index.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(emitterId);
    }

    private void unindex(ConcurrentMap<Long, Set<String>> index, Long key, String emitterId) {
        if (key == null) {
            return;
        }
        Set<String> emitterIds = index.get(key);
        if (emitterIds == null) {
            return;
        }
        emitterIds.remove(emitterId);
        if (emitterIds.isEmpty()) {
            index.remove(key, emitterIds);
        }
    }

    private List<RealtimeConnection> findByIndex(ConcurrentMap<Long, Set<String>> index, Long key) {
        if (key == null) {
            return Collections.emptyList();
        }
        Set<String> emitterIds = index.get(key);
        if (emitterIds == null || emitterIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<RealtimeConnection> result = new ArrayList<>();
        for (String emitterId : emitterIds) {
            RealtimeConnection connection = connections.get(emitterId);
            if (connection != null) {
                result.add(connection);
            }
        }
        return result;
    }
}
