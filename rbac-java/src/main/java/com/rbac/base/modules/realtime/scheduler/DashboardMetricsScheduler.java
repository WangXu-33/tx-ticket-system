package com.rbac.base.modules.realtime.scheduler;

import com.rbac.base.modules.realtime.service.RealtimeMessageService;
import com.rbac.base.modules.system.service.DashboardService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DashboardMetricsScheduler {

    private final DashboardService dashboardService;
    private final RealtimeMessageService messageService;

    public DashboardMetricsScheduler(DashboardService dashboardService, RealtimeMessageService messageService) {
        this.dashboardService = dashboardService;
        this.messageService = messageService;
    }

    @Scheduled(fixedDelayString = "${rbac.realtime.dashboard-metrics-delay:3000}")
    void broadcastServerMetrics() {
        // 首页探针只需要服务端单向推送，用 SSE 定时广播即可，不需要 WebSocket 双向通道。
        messageService.sendAll("server-metrics", dashboardService.getServerMetrics());
    }
}
