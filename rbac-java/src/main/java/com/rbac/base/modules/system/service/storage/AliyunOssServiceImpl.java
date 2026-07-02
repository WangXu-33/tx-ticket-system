package com.rbac.base.modules.system.service.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.rbac.base.modules.system.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service("aliyunOssService")
public class AliyunOssServiceImpl implements StorageService {

    @Autowired
    private SysConfigService configService;

    private String getConfig(String key) {
        if (key.contains("Secret")) {
            return configService.getDecryptedConfigValue(key);
        }
        return configService.getConfigValue(key);
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        String endpoint = getConfig("aliyun.oss.endpoint");
        String accessKeyId = getConfig("aliyun.oss.accessKeyId");
        String accessKeySecret = getConfig("aliyun.oss.accessKeySecret");
        String bucketName = getConfig("aliyun.oss.bucketName");
        String customDomain = getConfig("aliyun.oss.customDomain");

        if (endpoint == null || endpoint.isEmpty() || accessKeyId == null || accessKeySecret == null) {
            throw new RuntimeException("Aliyun OSS configuration is missing or incomplete in database.");
        }

        // Generate unique file name
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String datePath = LocalDate.now().toString().replace("-", "/");
        String objectName = "upload/" + datePath + "/" + uuid + suffix;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
            ossClient.putObject(putObjectRequest);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        // Return URL
        if (customDomain != null && !customDomain.isEmpty()) {
            return (customDomain.endsWith("/") ? customDomain : customDomain + "/") + objectName;
        } else {
            return "https://" + bucketName + "." + endpoint.replace("https://", "").replace("http://", "") + "/" + objectName;
        }
    }

    @Override
    public boolean delete(String fileUrl) {
        String endpoint = getConfig("aliyun.oss.endpoint");
        String accessKeyId = getConfig("aliyun.oss.accessKeyId");
        String accessKeySecret = getConfig("aliyun.oss.accessKeySecret");
        String bucketName = getConfig("aliyun.oss.bucketName");
        String customDomain = getConfig("aliyun.oss.customDomain");

        if (endpoint == null || endpoint.isEmpty()) {
            return false;
        }
        
        try {
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // Extract objectName from URL
            String domain = (customDomain != null && !customDomain.isEmpty()) 
                ? customDomain 
                : "https://" + bucketName + "." + endpoint.replace("https://", "").replace("http://", "");
            
            if (fileUrl.startsWith(domain)) {
                String objectName = fileUrl.substring(domain.length());
                if (objectName.startsWith("/")) {
                    objectName = objectName.substring(1);
                }
                ossClient.deleteObject(bucketName, objectName);
            }
            ossClient.shutdown();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getStorageName() {
        return "ALIYUN";
    }
}
