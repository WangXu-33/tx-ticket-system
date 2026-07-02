package com.rbac.base.modules.log.service;

import com.rbac.base.core.common.report.LoginLogReportQuery;
import com.rbac.base.core.common.report.OperLogReportQuery;
import com.rbac.base.core.common.report.ReportQueryUtils;
import com.rbac.base.core.common.vo.NameValueVO;
import com.rbac.base.core.common.vo.TrendPointVO;
import com.rbac.base.modules.log.entity.SysLoginLog;
import com.rbac.base.modules.log.entity.SysOperLog;
import com.rbac.base.modules.log.mapper.SysLoginLogMapper;
import com.rbac.base.modules.log.mapper.SysOperLogMapper;
import com.rbac.base.modules.log.vo.LoginLogSummaryVO;
import com.rbac.base.modules.log.vo.OperLogSummaryVO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LogReportService {

    private final SysLoginLogMapper loginLogMapper;
    private final SysOperLogMapper operLogMapper;

    public LogReportService(SysLoginLogMapper loginLogMapper, SysOperLogMapper operLogMapper) {
        this.loginLogMapper = loginLogMapper;
        this.operLogMapper = operLogMapper;
    }

    public LoginLogSummaryVO getLoginSummary(String username, String status, String startDate, String endDate) {
        LoginLogReportQuery query = buildLoginQuery(username, status, startDate, endDate);
        LoginLogSummaryVO summary = loginLogMapper.selectSummary(query);
        summary.setStatusPie(loginLogMapper.selectStatusPie(query));
        return summary;
    }

    public List<TrendPointVO> getLoginTrend(String username, String status, String startDate, String endDate, Integer days) {
        var range = ReportQueryUtils.resolveRange(startDate, endDate, days, 30);
        LoginLogReportQuery query = buildLoginQuery(username, status, range.getStartDateTime(), range.getEndDateTime());
        return fillTrend(range.getStartDate(), range.getEndDate(), loginLogMapper.selectTrend(query));
    }

    public List<NameValueVO> loadLoginStatusPie(String username, String startDate, String endDate) {
        return loginLogMapper.selectStatusPie(buildLoginQuery(username, null, startDate, endDate));
    }

    public List<SysLoginLog> listLoginExportRows(String username, String status, String startDate, String endDate) {
        return loginLogMapper.selectExportRows(buildLoginQuery(username, status, startDate, endDate));
    }

    public OperLogSummaryVO getOperSummary(String title, String operName, Integer businessType, Integer status, String startDate, String endDate) {
        return operLogMapper.selectSummary(buildOperQuery(title, operName, businessType, status, startDate, endDate));
    }

    public List<TrendPointVO> getOperTrend(String title, String operName, Integer businessType, Integer status, String startDate, String endDate, Integer days) {
        var range = ReportQueryUtils.resolveRange(startDate, endDate, days, 30);
        OperLogReportQuery query = buildOperQuery(title, operName, businessType, status, range.getStartDateTime(), range.getEndDateTime());
        return fillTrend(range.getStartDate(), range.getEndDate(), operLogMapper.selectTrend(query));
    }

    public List<NameValueVO> getOperTopModules(String title, String operName, Integer businessType, Integer status, String startDate, String endDate) {
        return operLogMapper.selectTopModules(buildOperQuery(title, operName, businessType, status, startDate, endDate));
    }

    public List<SysOperLog> listOperExportRows(String title, String operName, Integer businessType, Integer status, String startDate, String endDate) {
        return operLogMapper.selectExportRows(buildOperQuery(title, operName, businessType, status, startDate, endDate));
    }

    private LoginLogReportQuery buildLoginQuery(String username, String status, String startDate, String endDate) {
        var range = ReportQueryUtils.resolveRange(startDate, endDate, null, 30);
        return buildLoginQuery(username, status, range.getStartDateTime(), range.getEndDateTime());
    }

    private LoginLogReportQuery buildLoginQuery(String username, String status, java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime) {
        LoginLogReportQuery query = new LoginLogReportQuery();
        query.setUsername(username);
        query.setStatus(status == null || status.isBlank() ? null : Integer.parseInt(status));
        query.setStartDateTime(startDateTime);
        query.setEndDateTime(endDateTime);
        return query;
    }

    private OperLogReportQuery buildOperQuery(String title, String operName, Integer businessType, Integer status, String startDate, String endDate) {
        var range = ReportQueryUtils.resolveRange(startDate, endDate, null, 30);
        return buildOperQuery(title, operName, businessType, status, range.getStartDateTime(), range.getEndDateTime());
    }

    private OperLogReportQuery buildOperQuery(String title, String operName, Integer businessType, Integer status, java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime) {
        OperLogReportQuery query = new OperLogReportQuery();
        query.setTitle(title);
        query.setOperName(operName);
        query.setBusinessType(businessType);
        query.setStatus(status);
        query.setStartDateTime(startDateTime);
        query.setEndDateTime(endDateTime);
        return query;
    }

    private List<TrendPointVO> fillTrend(LocalDate startDate, LocalDate endDate, List<TrendPointVO> dbRows) {
        Map<LocalDate, Long> valueMap = new LinkedHashMap<>();
        dbRows.forEach(item -> valueMap.put(LocalDate.parse(item.getDate()), item.getValue()));
        return ReportQueryUtils.fillTrend(startDate, endDate, valueMap);
    }
}
