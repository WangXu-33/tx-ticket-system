package com.rbac.base.core.common.report;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class FileReportQuery {
    private String fileName;
    private String storageType;
    private List<String> fileSuffixes;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
}
