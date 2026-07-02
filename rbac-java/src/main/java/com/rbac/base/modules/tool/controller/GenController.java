package com.rbac.base.modules.tool.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.rbac.base.core.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 可视化代码生成器
 */
@RestController
@RequestMapping("/tool/gen")
public class GenController {

    @GetMapping("/tableList")
    @SaCheckPermission("tool:gen:list")
    public Result<?> getTableList() {
        return Result.error("当前内置代码生成器已冻结，请改用独立 rbac-codegen 服务");
    }

    @GetMapping("/preview")
    @SaCheckPermission("tool:gen:preview")
    public Result<?> preview(@RequestParam String tableName, @RequestParam String moduleDesc) {
        return Result.error("当前内置代码生成器已冻结，请改用独立 rbac-codegen 服务");
    }

    @PostMapping("/execute")
    @SaCheckPermission("tool:gen:execute")
    public Result<?> execute(@RequestBody Map<String, String> params) {
        return Result.error("当前内置代码生成器已冻结，请改用独立 rbac-codegen 服务");
    }
}
