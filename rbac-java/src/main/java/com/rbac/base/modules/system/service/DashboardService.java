package com.rbac.base.modules.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.rbac.base.core.common.report.ReportQueryUtils;
import com.rbac.base.core.common.vo.TrendPointVO;
import com.rbac.base.modules.system.mapper.DashboardMapper;
import com.rbac.base.modules.system.vo.DashboardDistributionVO;
import com.rbac.base.modules.system.vo.DashboardOverviewVO;
import com.rbac.base.modules.system.vo.DashboardTrendVO;
import com.rbac.base.modules.system.vo.ServerMetricsVO;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    public DashboardService(DashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardOverviewVO getOverview() {
        DashboardOverviewVO overview = dashboardMapper.selectOverview();
        overview.setOnlineUserTotal((long) StpUtil.searchTokenValue("", 0, 100000, false).size());
        return overview;
    }

    public DashboardTrendVO getTrend(Integer days) {
        var range = ReportQueryUtils.resolveRange(null, null, days, 30);
        DashboardTrendVO trend = new DashboardTrendVO();
        trend.setLoginTrend(fillTrend(range.getStartDate(), range.getEndDate(), dashboardMapper.selectLoginTrend(range.getStartDateTime(), range.getEndDateTime())));
        trend.setOperTrend(fillTrend(range.getStartDate(), range.getEndDate(), dashboardMapper.selectOperTrend(range.getStartDateTime(), range.getEndDateTime())));
        trend.setFileUploadTrend(fillTrend(range.getStartDate(), range.getEndDate(), dashboardMapper.selectFileUploadTrend(range.getStartDateTime(), range.getEndDateTime())));
        return trend;
    }

    public DashboardDistributionVO getDistribution() {
        DashboardDistributionVO distribution = new DashboardDistributionVO();
        distribution.setLoginStatusPie(dashboardMapper.selectLoginStatusPie());
        distribution.setFileStoragePie(dashboardMapper.selectFileStoragePie());
        distribution.setDeptUserTop(dashboardMapper.selectDeptUserTop());
        distribution.setRoleUserTop(dashboardMapper.selectRoleUserTop());
        return distribution;
    }

    public ServerMetricsVO getServerMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        Runtime runtime = Runtime.getRuntime();
        ServerMetricsVO metrics = new ServerMetricsVO();
        metrics.setCpuUsage(roundPercent(osBean.getSystemCpuLoad() * 100));
        long totalMemory = osBean.getTotalPhysicalMemorySize();
        long freeMemory = osBean.getFreePhysicalMemorySize();
        double memoryUsage = totalMemory <= 0 ? 0D : ((double) (totalMemory - freeMemory) / totalMemory) * 100;
        metrics.setMemoryUsage(roundPercent(memoryUsage));
        metrics.setDiskUsage(roundPercent(resolveDiskUsage()));
        metrics.setJvmUsed((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        metrics.setJvmMax(runtime.maxMemory() / 1024 / 1024);
        return metrics;
    }

    private List<TrendPointVO> fillTrend(LocalDate startDate, LocalDate endDate, List<TrendPointVO> dbRows) {
        Map<LocalDate, Long> valueMap = new LinkedHashMap<>();
        dbRows.forEach(item -> valueMap.put(LocalDate.parse(item.getDate()), item.getValue()));
        return ReportQueryUtils.fillTrend(startDate, endDate, valueMap);
    }

    private Double roundPercent(double value) {
        return Math.round(Math.max(value, 0D) * 10D) / 10D;
    }

    private double resolveDiskUsage() {
        File current = new File(".");
        long total = current.getTotalSpace();
        long free = current.getUsableSpace();
        if (total <= 0) {
            return 0D;
        }
        return ((double) (total - free) / total) * 100;
    }
}
