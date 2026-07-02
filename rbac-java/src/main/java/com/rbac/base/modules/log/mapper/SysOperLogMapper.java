package com.rbac.base.modules.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.core.common.report.OperLogReportQuery;
import com.rbac.base.core.common.vo.NameValueVO;
import com.rbac.base.core.common.vo.TrendPointVO;
import com.rbac.base.modules.log.entity.SysOperLog;
import com.rbac.base.modules.log.vo.OperLogSummaryVO;

import java.util.List;

public interface SysOperLogMapper extends BaseMapper<SysOperLog> {
    OperLogSummaryVO selectSummary(OperLogReportQuery query);

    List<TrendPointVO> selectTrend(OperLogReportQuery query);

    List<NameValueVO> selectTopModules(OperLogReportQuery query);

    List<SysOperLog> selectExportRows(OperLogReportQuery query);
}
