package com.rbac.base.modules.system.service.storage;

import com.rbac.base.modules.system.service.SysConfigService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Service("minioService")
public class MinioServiceImpl implements StorageService {

    @Autowired
    private SysConfigService configService;

    private String getConfig(String key) {
        if (key.contains("Secret") || key.contains("secretKey")) {
            return configService.getDecryptedConfigValue(key);
        }
        return configService.getConfigValue(key);
    }

    private MinioClient buildClient() {
        String endpoint = getConfig("minio.endpoint");
        String accessKey = getConfig("minio.accessKey");
        String secretKey = getConfig("minio.secretKey");
        String useHttpsStr = getConfig("minio.useHttps");
        boolean useHttps = "true".equalsIgnoreCase(useHttpsStr) || "1".equals(useHttpsStr);
        
        if (endpoint == null || endpoint.isEmpty() || accessKey == null || secretKey == null) {
            throw new RuntimeException("MinIO configuration is missing or incomplete in database.");
        }
        
        String protocol = useHttps ? "https://" : "http://";
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
             protocol = ""; // 已经包含了协议
        }

        return MinioClient.builder()
                .endpoint(protocol + endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Override
    public String upload(MultipartFile file) throws IOException {
        String bucketName = getConfig("minio.bucketName");
        if (bucketName == null || bucketName.isEmpty()) {
            throw new RuntimeException("MinIO bucketName configuration is missing.");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String datePath = LocalDate.now().toString().replace("-", "/");
        String objectName = "upload/" + datePath + "/" + uuid + suffix;

        MinioClient minioClient = buildClient();

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception e) {
            throw new IOException("Failed to upload file to MinIO", e);
        }

        String endpoint = getConfig("minio.endpoint");
        String useHttpsStr = getConfig("minio.useHttps");
        boolean useHttps = "true".equalsIgnoreCase(useHttpsStr) || "1".equals(useHttpsStr);
        String protocol = useHttps ? "https://" : "http://";
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
             protocol = ""; 
        }
        
        String customDomain = getConfig("minio.customDomain");
        if (customDomain != null && !customDomain.isEmpty()) {
             return (customDomain.endsWith("/") ? customDomain : customDomain + "/") + bucketName + "/" + objectName;
        }

        return protocol + endpoint + "/" + bucketName + "/" + objectName;
    }

    @Override
    public boolean delete(String fileUrl) {
        String bucketName = getConfig("minio.bucketName");
        if (bucketName == null || bucketName.isEmpty() || fileUrl == null) {
            return false;
        }

        try {
            MinioClient minioClient = buildClient();
            
            // Extract objectName. Example URL: http://ip:9000/bucketName/upload/2023/10/...
            String searchStr = "/" + bucketName + "/";
            int idx = fileUrl.indexOf(searchStr);
            if (idx != -1) {
                String objectName = fileUrl.substring(idx + searchStr.length());
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
                );
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getStorageName() {
        return "MINIO";
    }
}
