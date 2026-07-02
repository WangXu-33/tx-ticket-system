package com.rbac.base.modules.system.controller;

import com.rbac.base.core.common.Result;
import com.rbac.base.modules.system.entity.SysFile;
import com.rbac.base.modules.system.service.SysConfigService;
import com.rbac.base.modules.system.service.SysFileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/system")
public class UploadController {

    @Autowired
    private SysConfigService configService;

    @Autowired
    private SysFileService sysFileService;

    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传失败，请选择文件");
        }

        try {
            SysFile sysFile = sysFileService.uploadAndSave(file);
            return Result.success(sysFile);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 通用下载接口
     * @param fileName 相对路径文件名，如 2026/04/27/xxx.png
     */
    @GetMapping("/download")
    public void download(String fileName, HttpServletResponse response) throws IOException {
        if (!StringUtils.hasText(fileName)) {
            return;
        }

        String rootPath = configService.getConfigValue("local.storage.path");
        if (!StringUtils.hasText(rootPath)) {
            rootPath = "E:/tx_ticket_upload/";
        }

        Path root = Paths.get(rootPath).toAbsolutePath().normalize();
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            response.sendError(400, "非法文件路径");
            return;
        }

        File file = target.toFile();
        if (!file.exists()) {
            response.sendError(404, "文件不存在");
            return;
        }

        response.reset();
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setContentLength((int) file.length());

        String downloadName = file.getName();
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(downloadName, "UTF-8"));

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] b = new byte[1024];
            int len;
            while ((len = fis.read(b)) > 0) {
                os.write(b, 0, len);
            }
        }
    }
}
