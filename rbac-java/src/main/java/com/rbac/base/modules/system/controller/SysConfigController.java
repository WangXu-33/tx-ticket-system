package com.rbac.base.modules.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.core.common.Result;
import com.rbac.base.modules.system.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/system/config")
public class SysConfigController {

    @Autowired
    private SysConfigService configService;

    @GetMapping("/getOssConfig")
    @SaCheckPermission("sys:config:list")
    public Result<?> getOssConfig() {
        Map<String, String> configs = configService.getConfigsByPrefix("aliyun.oss");
        configs.putAll(configService.getConfigsByPrefix("minio"));
        configs.putAll(configService.getConfigsByPrefix("local.storage"));
        configs.putAll(configService.getConfigsByPrefix("file.security"));
        configs.put("system.storage.active", configService.getConfigValue("system.storage.active"));
        configs.putIfAbsent("file.security.max-size-mb", "20");
        configs.putIfAbsent("file.security.allowed-suffixes", ".jpg,.jpeg,.png,.gif,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar");
        configs.putIfAbsent("file.security.blocked-suffixes", ".exe,.bat,.cmd,.sh,.ps1,.jar,.war,.msi,.dll,.com,.scr");

        // Mask sensitive data
        if (configs.containsKey("aliyun.oss.accessKeySecret") && !configs.get("aliyun.oss.accessKeySecret").isEmpty()) {
            configs.put("aliyun.oss.accessKeySecret", "********");
        }
        if (configs.containsKey("minio.secretKey") && !configs.get("minio.secretKey").isEmpty()) {
            configs.put("minio.secretKey", "********");
        }
        
        return Result.success(configs);
    }

    @PostMapping("/saveOssConfig")
    @SaCheckPermission("sys:config:edit")
    @Log(title = "存储配置", businessType = 2)
    public Result<?> saveOssConfig(@RequestBody Map<String, String> params) {
        configService.saveConfigs(params);
        return Result.success();
    }
}
