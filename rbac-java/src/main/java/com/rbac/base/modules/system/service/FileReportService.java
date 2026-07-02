package com.rbac.base.modules.system.service;

import com.rbac.base.core.common.report.FileReportQuery;
import com.rbac.base.core.common.report.ReportQueryUtils;
import com.rbac.base.core.common.vo.TrendPointVO;
import com.rbac.base.modules.system.entity.SysFile;
import com.rbac.base.modules.system.mapper.SysFileMapper;
import com.rbac.base.modules.system.vo.FileStatsSummaryVO;
import com.rbac.base.modules.system.vo.FileTypeDistributionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileReportService {

    private final SysFileMapper fileMapper;

    public FileReportService(SysFileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    public FileStatsSummaryVO getSummary(String fileName, String storageType, String fileSuffixes, String startDate, String endDate) {
        return fileMapper.selectSummary(buildQuery(fileName, storageType, parseSuffixes(fileSuffixes), startDate, endDate));
    }

    public List<TrendPointVO> getTrend(String fileName, String storageType, String fileSuffixes, String startDate, String endDate, Integer days) {
        var range = ReportQueryUtils.resolveRange(startDate, endDate, days, 30);
        FileReportQuery query = buildRangeQuery(fileName, storageType, parseSuffixes(fileSuffixes), range.getStartDateTime(), range.getEndDateTime());
        return fillTrend(range.getStartDate(), range.getEndDate(), fileMapper.selectTrend(query));
    }

    public FileTypeDistributionVO getTypeDistribution(String fileName, String storageType, String fileSuffixes, String startDate, String endDate) {
        FileReportQuery query = buildQuery(fileName, storageType, parseSuffixes(fileSuffixes), startDate, endDate);
        FileTypeDistributionVO distribution = new FileTypeDistributionVO();
        distribution.setStorageTypePie(fileMapper.selectStoragePie(query));
        distribution.setSuffixTop(fileMapper.selectSuffixTop(query));
        return distribution;
    }

    public List<SysFile> listExportRows(String fileName, String storageType, String fileSuffixes, String startDate, String endDate) {
        return fileMapper.selectExportRows(buildQuery(fileName, storageType, parseSuffixes(fileSuffixes), startDate, endDate));
    }

    private FileReportQuery buildQuery(String fileName, String storageType, List<String> fileSuffixes, String startDate, String endDate) {
        if (StringUtils.hasText(startDate) && StringUtils.hasText(endDate)) {
            var range = ReportQueryUtils.resolveRange(startDate, endDate, null, 30);
            return buildRangeQuery(fileName, storageType, fileSuffixes, range.getStartDateTime(), range.getEndDateTime());
        }
        return buildRangeQuery(fileName, storageType, fileSuffixes, null, null);
    }

    private FileReportQuery buildRangeQuery(String fileName, String storageType, List<String> fileSuffixes, java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime) {
        FileReportQuery query = new FileReportQuery();
        query.setFileName(fileName);
        query.setStorageType(storageType);
        query.setFileSuffixes(fileSuffixes);
        query.setStartDateTime(startDateTime);
        query.setEndDateTime(endDateTime);
        return query;
    }

    private List<String> parseSuffixes(String fileSuffixes) {
        if (!StringUtils.hasText(fileSuffixes)) {
            return List.of();
        }
        return Arrays.stream(fileSuffixes.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .map(item -> "__NONE__".equals(item) ? "" : item)
                .toList();
    }

    private List<TrendPointVO> fillTrend(LocalDate startDate, LocalDate endDate, List<TrendPointVO> dbRows) {
        Map<LocalDate, Long> valueMap = new LinkedHashMap<>();
        dbRows.forEach(item -> valueMap.put(LocalDate.parse(item.getDate()), item.getValue()));
        return ReportQueryUtils.fillTrend(startDate, endDate, valueMap);
    }
}
