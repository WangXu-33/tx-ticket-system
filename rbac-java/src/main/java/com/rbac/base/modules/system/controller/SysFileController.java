package com.rbac.base.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.core.common.report.ExportUtils;
import com.rbac.base.modules.system.entity.SysFile;
import com.rbac.base.modules.system.mapper.SysFileMapper;
import com.rbac.base.modules.system.service.FileReportService;
import com.rbac.base.modules.system.service.SysFileService;
import com.rbac.base.modules.system.service.storage.StorageService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/system/file")
public class SysFileController {

    @Autowired
    private SysFileMapper fileMapper;

    @Autowired
    private SysFileService sysFileService;

    @Autowired
    private FileReportService fileReportService;

    @GetMapping("/list")
    @SaCheckPermission("sys:file:list")
    public Result<?> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String storageType,
            @RequestParam(required = false) String fileSuffixes,
            @RequestParam(required = false) String beginTime,
            @RequestParam(required = false) String endTime) {
        
        Page<SysFile> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysFile> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(fileName)) {
            wrapper.like(SysFile::getFileName, fileName);
        }
        if (StringUtils.hasText(storageType)) {
            wrapper.eq(SysFile::getStorageType, storageType);
        }
        List<String> suffixList = parseSuffixes(fileSuffixes);
        if (!suffixList.isEmpty()) {
            wrapper.in(SysFile::getFileSuffix, suffixList);
        }
        if (StringUtils.hasText(beginTime) && StringUtils.hasText(endTime)) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            wrapper.between(SysFile::getCreateTime, LocalDateTime.parse(beginTime, fmt), LocalDateTime.parse(endTime, fmt));
        }
        
        wrapper.orderByDesc(SysFile::getId);
        return Result.success(fileMapper.selectPage(page, wrapper));
    }

    @GetMapping("/suffix-options")
    @SaCheckPermission("sys:file:list")
    public Result<?> suffixOptions() {
        return Result.success(fileMapper.selectSuffixOptions());
    }

    @GetMapping("/stats")
    @SaCheckPermission("sys:file:list")
    public Result<?> stats(@RequestParam(required = false) String fileName,
                           @RequestParam(required = false) String storageType,
                           @RequestParam(required = false) String fileSuffixes,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate) {
        return Result.success(fileReportService.getSummary(fileName, storageType, fileSuffixes, startDate, endDate));
    }

    @GetMapping("/stats/trend")
    @SaCheckPermission("sys:file:list")
    public Result<?> trend(@RequestParam(required = false) String fileName,
                           @RequestParam(required = false) String storageType,
                           @RequestParam(required = false) String fileSuffixes,
                           @RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(fileReportService.getTrend(fileName, storageType, fileSuffixes, startDate, endDate, days));
    }

    @GetMapping("/stats/type")
    @SaCheckPermission("sys:file:list")
    public Result<?> typeStats(@RequestParam(required = false) String fileName,
                               @RequestParam(required = false) String storageType,
                               @RequestParam(required = false) String fileSuffixes,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        return Result.success(fileReportService.getTypeDistribution(fileName, storageType, fileSuffixes, startDate, endDate));
    }

    @GetMapping("/export")
    @SaCheckPermission("sys:file:export")
    public void export(@RequestParam(defaultValue = "xlsx") String format,
                       @RequestParam(required = false) String fileName,
                       @RequestParam(required = false) String storageType,
                       @RequestParam(required = false) String fileSuffixes,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws IOException {
        var rows = fileReportService.listExportRows(fileName, storageType, fileSuffixes, startDate, endDate);
        java.util.List<? extends java.util.List<?>> exportRows = rows.stream()
                .map(item -> (java.util.List<?>) java.util.Arrays.asList(
                        item.getId(),
                        item.getFileName(),
                        item.getFileSuffix(),
                        item.getFileSize(),
                        item.getStorageType(),
                        item.getUrl(),
                        item.getCreateTime()
                ))
                .toList();
        ExportUtils.write(
                format,
                "file-report",
                java.util.List.of("编号", "文件名称", "文件后缀", "文件大小", "存储类型", "文件地址", "上传时间"),
                exportRows,
                response
        );
    }

    @GetMapping("/preview/{id}")
    public void previewFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile == null) {
            response.sendError(404, "文件不存在");
            return;
        }

        if (!sysFileService.isLocalStorage(sysFile)) {
            response.sendRedirect(sysFile.getUrl());
            return;
        }

        writeLocalFile(response, sysFile, false);
    }

    @GetMapping("/download/{id}")
    public void downloadFile(@PathVariable Long id, HttpServletResponse response) throws IOException {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile == null) {
            response.sendError(404, "文件不存在");
            return;
        }

        if (!sysFileService.isLocalStorage(sysFile)) {
            response.sendRedirect(sysFile.getUrl());
            return;
        }

        writeLocalFile(response, sysFile, true);
    }

    @PostMapping("/upload")
    @SaCheckPermission("sys:file:upload")
    @Log(title = "文件管理", businessType = 1)
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        
        try {
            SysFile sysFile = sysFileService.uploadAndSave(file);
            return Result.success(sysFile);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @SaCheckPermission("sys:file:delete")
    @Log(title = "文件管理", businessType = 3)
    public Result<?> delete(@PathVariable Long id) {
        SysFile sysFile = fileMapper.selectById(id);
        if (sysFile != null) {
            try {
                StorageService storageService = sysFileService.getStorageService(sysFile);
                storageService.delete(sysFile.getUrl());
            } catch (Exception e) {
                e.printStackTrace();
            }
            fileMapper.deleteById(id);
        }
        return Result.success();
    }

    private void writeLocalFile(HttpServletResponse response, SysFile sysFile, boolean download) throws IOException {
        Path filePath;
        try {
            filePath = sysFileService.resolveLocalPath(sysFile);
        } catch (IllegalArgumentException e) {
            response.sendError(400, e.getMessage());
            return;
        }

        if (!Files.exists(filePath)) {
            response.sendError(404, "文件不存在");
            return;
        }

        String contentType = Files.probeContentType(filePath);
        if (!StringUtils.hasText(contentType)) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        response.reset();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(contentType);
        response.setHeader("Content-Length", String.valueOf(Files.size(filePath)));

        String dispositionType = download ? "attachment" : "inline";
        String fileName = StringUtils.hasText(sysFile.getFileName()) ? sysFile.getFileName() : filePath.getFileName().toString();
        response.setHeader(
                "Content-Disposition",
                dispositionType + ";filename=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8)
        );

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            inputStream.transferTo(response.getOutputStream());
        }
    }

    private List<String> parseSuffixes(String fileSuffixes) {
        if (!StringUtils.hasText(fileSuffixes)) {
            return List.of();
        }
        return Arrays.stream(fileSuffixes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(item -> "__NONE__".equals(item) ? "" : item)
                .toList();
    }
}
