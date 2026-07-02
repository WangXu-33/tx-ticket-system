package com.rbac.base.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rbac.base.core.common.report.FileReportQuery;
import com.rbac.base.core.common.vo.NameValueVO;
import com.rbac.base.core.common.vo.TrendPointVO;
import com.rbac.base.modules.system.entity.SysFile;
import com.rbac.base.modules.system.vo.FileSuffixOptionVO;
import com.rbac.base.modules.system.vo.FileStatsSummaryVO;

import java.util.List;

public interface SysFileMapper extends BaseMapper<SysFile> {
    FileStatsSummaryVO selectSummary(FileReportQuery query);

    List<TrendPointVO> selectTrend(FileReportQuery query);

    List<NameValueVO> selectStoragePie(FileReportQuery query);

    List<NameValueVO> selectSuffixTop(FileReportQuery query);

    List<FileSuffixOptionVO> selectSuffixOptions();

    List<SysFile> selectExportRows(FileReportQuery query);
}
