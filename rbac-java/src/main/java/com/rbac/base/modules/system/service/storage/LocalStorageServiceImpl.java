package com.rbac.base.modules.system.service.storage;

import com.rbac.base.modules.system.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.UUID;

@Service("localStorageService")
public class LocalStorageServiceImpl implements StorageService {

    @Autowired
    private SysConfigService configService;

    private String getRootPath() {
        String path = configService.getConfigValue("local.storage.path");
        return StringUtils.hasText(path) ? path : "E:/tx_ticket_upload/";
    }

    private String getDomain() {
        String domain = configService.getConfigValue("local.storage.domain");
        if (!StringUtils.hasText(domain)) {
            return "/tx_files/";
        }

        String normalized = domain.trim().replace("\\", "/");
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            try {
                normalized = new URI(normalized).getPath();
            } catch (URISyntaxException ignored) {
                normalized = "/tx_files/";
            }
        }

        // 本地入库地址必须是静态资源前缀，不能使用按 ID 访问的预览接口前缀。
        if (normalized.contains("/system/file/preview") || normalized.contains("/system/file/download")) {
            return "/tx_files/";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        String rootPath = getRootPath();
        String domain = getDomain();

        String datePath = LocalDate.now().toString().replace("-", "/");
        File dir = new File(rootPath, datePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
            : "";
            
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        File dest = new File(dir, newFilename);
        
        file.transferTo(dest);
        
        // 存为相对路径，访问时通过 /preview/{id} 重定向，或者存物理相对路径后拼前缀
        // 由于需要入库 sys_file 表，此时还没产生 ID，所以存为完整 URL。
        // 如果我们用 ID 去访问，那么 domain 其实没那么重要，但为了兼容之前的逻辑，我们存一个约定的虚拟URL
        String domainSuffix = domain.endsWith("/") ? "" : "/";
        return domain + domainSuffix + datePath + "/" + newFilename;
    }

    @Override
    public boolean delete(String fileUrl) {
        String domain = getDomain();
        String domainSuffix = domain.endsWith("/") ? "" : "/";
        String fullDomain = domain + domainSuffix;

        if (fileUrl != null && fileUrl.startsWith(fullDomain)) {
            String filename = fileUrl.substring(fullDomain.length());
            File file = new File(getRootPath(), filename);
            return file.exists() && file.delete();
        }
        return false;
    }

    @Override
    public String getStorageName() {
        return "LOCAL";
    }
}
