package com.rbac.base.modules.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.core.common.report.LoginLogReportQuery;
import com.rbac.base.core.common.vo.NameValueVO;
import com.rbac.base.core.common.vo.TrendPointVO;
import com.rbac.base.modules.log.entity.SysLoginLog;
import com.rbac.base.modules.log.vo.LoginLogSummaryVO;

import java.util.List;

public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
    LoginLogSummaryVO selectSummary(LoginLogReportQuery query);

    List<TrendPointVO> selectTrend(LoginLogReportQuery query);

    List<NameValueVO> selectStatusPie(LoginLogReportQuery query);

    List<SysLoginLog> selectExportRows(LoginLogReportQuery query);
}
