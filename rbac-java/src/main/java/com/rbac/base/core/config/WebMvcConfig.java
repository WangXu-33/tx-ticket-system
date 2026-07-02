package com.rbac.base.core.config;

import com.rbac.base.modules.system.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private SysConfigService configService;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String rootPath = configService.getConfigValue("local.storage.path");
        if (!StringUtils.hasText(rootPath)) {
            rootPath = "E:/tx_ticket_upload/";
        }

        String resourceLocation = Paths.get(rootPath).toAbsolutePath().normalize().toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation = resourceLocation + "/";
        }

        registry.addResourceHandler("/tx_files/**")
                .addResourceLocations(resourceLocation)
                .setCachePeriod(0);
    }
}
