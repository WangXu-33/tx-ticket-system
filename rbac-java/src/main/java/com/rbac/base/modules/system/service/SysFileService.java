package com.rbac.base.modules.system.service;

import com.rbac.base.modules.system.entity.SysFile;
import com.rbac.base.modules.system.mapper.SysFileMapper;
import com.rbac.base.modules.system.service.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;

@Service
public class SysFileService {

    @Autowired
    private SysFileMapper fileMapper;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SysConfigService configService;

    public SysFile uploadAndSave(MultipartFile file) throws IOException {
        validateUploadFile(file);
        StorageService storageService = getActiveStorageService();
        String fileUrl = storageService.upload(file);

        SysFile sysFile = new SysFile();
        sysFile.setFileName(file.getOriginalFilename());
        sysFile.setFileSuffix(extractSuffix(file.getOriginalFilename()));
        sysFile.setFileSize(file.getSize());
        sysFile.setUrl(fileUrl);
        sysFile.setStorageType(storageService.getStorageName());
        fileMapper.insert(sysFile);
        return sysFile;
    }

    public StorageService getStorageService(SysFile sysFile) {
        if (sysFile == null || !StringUtils.hasText(sysFile.getStorageType())) {
            return getActiveStorageService();
        }
        return switch (sysFile.getStorageType().toUpperCase()) {
            case "ALIYUN" -> applicationContext.getBean("aliyunOssService", StorageService.class);
            case "MINIO" -> applicationContext.getBean("minioService", StorageService.class);
            default -> applicationContext.getBean("localStorageService", StorageService.class);
        };
    }

    public boolean isLocalStorage(SysFile sysFile) {
        return sysFile != null && "LOCAL".equalsIgnoreCase(sysFile.getStorageType());
    }

    public Path resolveLocalPath(SysFile sysFile) {
        String rootPath = configService.getConfigValue("local.storage.path");
        if (!StringUtils.hasText(rootPath)) {
            rootPath = "E:/tx_ticket_upload/";
        }

        String relativePath = extractRelativePath(sysFile.getUrl());
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("文件路径无效");
        }
        Path root = Paths.get(rootPath).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件路径");
        }
        return target;
    }

    private StorageService getActiveStorageService() {
        String active = configService.getConfigValue("system.storage.active");
        String beanName = StringUtils.hasText(active) ? active : "localStorageService";
        return applicationContext.getBean(beanName, StorageService.class);
    }

    /**
     * 代码修改时间：2026-07-02。
     * 功能说明：根据系统配置校验上传附件大小、允许后缀和禁止后缀。
     * 入参：MultipartFile 上传文件对象。
     * 出参：无返回，校验通过后继续上传。
     * 异常场景：文件超限或后缀不符合配置时抛出 IllegalArgumentException。
     */
    private void validateUploadFile(MultipartFile file) {
        long maxSizeBytes = resolveMaxSizeBytes();
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("文件大小不能超过 " + (maxSizeBytes / 1024 / 1024) + "MB");
        }

        String suffix = extractSuffix(file.getOriginalFilename()).toLowerCase(Locale.ROOT);
        List<String> blockedSuffixes = parseSuffixes(defaultConfig(
                configService.getConfigValue("file.security.blocked-suffixes"),
                ".exe,.bat,.cmd,.sh,.ps1,.jar,.war,.msi,.dll,.com,.scr"
        ));
        if (blockedSuffixes.contains(suffix)) {
            throw new IllegalArgumentException("当前文件类型禁止上传");
        }

        List<String> allowedSuffixes = parseSuffixes(defaultConfig(
                configService.getConfigValue("file.security.allowed-suffixes"),
                ".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar"
        ));
        if (!allowedSuffixes.isEmpty() && !allowedSuffixes.contains(suffix)) {
            throw new IllegalArgumentException("当前文件类型不在允许上传范围内");
        }
    }

    private long resolveMaxSizeBytes() {
        String value = configService.getConfigValue("file.security.max-size-mb");
        long maxSizeMb = 20L;
        if (StringUtils.hasText(value)) {
            try {
                maxSizeMb = Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
                maxSizeMb = 20L;
            }
        }
        if (maxSizeMb <= 0) {
            maxSizeMb = 20L;
        }
        return maxSizeMb * 1024 * 1024;
    }

    private List<String> parseSuffixes(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(item -> item.startsWith(".") ? item : "." + item)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private String defaultConfig(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String extractSuffix(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    private String extractRelativePath(String fileUrl) {
        String url = StringUtils.hasText(fileUrl) ? fileUrl.trim().replace("\\", "/") : "";
        if (!StringUtils.hasText(url)) {
            return "";
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                URI uri = new URI(url);
                url = uri.getPath();
            } catch (URISyntaxException ignored) {
                // keep original url and continue best-effort parsing
            }
        }

        String normalizedDomain = normalizeLocalStorageDomain(configService.getConfigValue("local.storage.domain"));

        String normalizedDomainWithoutSlash = normalizedDomain.endsWith("/")
                ? normalizedDomain.substring(0, normalizedDomain.length() - 1)
                : normalizedDomain;

        if (url.startsWith(normalizedDomain)) {
            url = url.substring(normalizedDomain.length());
        } else if (url.startsWith(normalizedDomainWithoutSlash + "/")) {
            url = url.substring(normalizedDomainWithoutSlash.length() + 1);
        } else if (url.equals(normalizedDomainWithoutSlash)) {
            url = "";
        } else {
            int index = url.indexOf("/tx_files/");
            if (index >= 0) {
                url = url.substring(index + "/tx_files/".length());
            } else {
                int noSlashIndex = url.indexOf("/tx_files");
                if (noSlashIndex >= 0) {
                    url = url.substring(noSlashIndex + "/tx_files".length());
                } else {
                    int previewIndex = url.indexOf("/system/file/preview/");
                    if (previewIndex >= 0) {
                        url = url.substring(previewIndex + "/system/file/preview/".length());
                    } else {
                        int downloadIndex = url.indexOf("/system/file/download/");
                        if (downloadIndex >= 0) {
                            url = url.substring(downloadIndex + "/system/file/download/".length());
                        }
                    }
                }
            }
        }

        while (url.startsWith("/")) {
            url = url.substring(1);
        }
        return url;
    }

    private String normalizeLocalStorageDomain(String domain) {
        String normalizedDomain = StringUtils.hasText(domain) ? domain.trim().replace("\\", "/") : "/tx_files/";
        if (normalizedDomain.startsWith("http://") || normalizedDomain.startsWith("https://")) {
            try {
                normalizedDomain = new URI(normalizedDomain).getPath();
            } catch (URISyntaxException ignored) {
                normalizedDomain = "/tx_files/";
            }
        }
        if (normalizedDomain.contains("/system/file/preview") || normalizedDomain.contains("/system/file/download")) {
            normalizedDomain = "/tx_files/";
        }
        if (!normalizedDomain.startsWith("/")) {
            normalizedDomain = "/" + normalizedDomain;
        }
        if (!normalizedDomain.endsWith("/")) {
            normalizedDomain = normalizedDomain + "/";
        }
        return normalizedDomain;
    }
}
