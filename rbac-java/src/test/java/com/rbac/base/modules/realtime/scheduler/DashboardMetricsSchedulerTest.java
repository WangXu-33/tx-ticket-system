package com.rbac.base.modules.realtime.scheduler;

import com.rbac.base.modules.realtime.service.RealtimeMessageService;
import com.rbac.base.modules.system.service.DashboardService;
import com.rbac.base.modules.system.vo.ServerMetricsVO;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardMetricsSchedulerTest {

    @Test
    void broadcastServerMetricsSendsMetricsToAllConnections() {
        DashboardService dashboardService = mock(DashboardService.class);
        RealtimeMessageService messageService = mock(RealtimeMessageService.class);
        DashboardMetricsScheduler scheduler = new DashboardMetricsScheduler(dashboardService, messageService);
        ServerMetricsVO metrics = new ServerMetricsVO();
        metrics.setCpuUsage(10.5D);
        when(dashboardService.getServerMetrics()).thenReturn(metrics);

        scheduler.broadcastServerMetrics();

        verify(messageService).sendAll("server-metrics", metrics);
    }
}
