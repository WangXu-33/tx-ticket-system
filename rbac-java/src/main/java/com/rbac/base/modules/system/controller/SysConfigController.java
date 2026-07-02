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
        configs.put("system.storage.active", configService.getConfigValue("system.storage.active"));

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
