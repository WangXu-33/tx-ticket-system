package com.rbac.base.modules.system.service.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * 统一附件存储服务接口
 * 支持多云厂商 (阿里云 OSS, 腾讯云 COS, MinIO) 以及本地存储
 */
public interface StorageService {
    
    /**
     * 上传文件
     * @param file 文件对象
     * @return 返回可访问的绝对 URL
     */
    String upload(MultipartFile file) throws IOException;
    
    /**
     * 删除文件
     * @param fileUrl 文件的 URL
     */
    boolean delete(String fileUrl);
    
    /**
     * 获取当前存储策略名称
     */
    String getStorageName();
}
