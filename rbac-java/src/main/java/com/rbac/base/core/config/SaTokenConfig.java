package com.rbac.base.core.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/**")
                    .notMatch("/auth/login")
                    .notMatch("/auth/register")
                    .check(r -> StpUtil.checkLogin());
        }))
        .addPathPatterns("/**")
        .excludePathPatterns(
                "/tx_files/**",
                "/system/file/preview/**",
                "/system/file/download/**",
                "/realtime/sse",
                "/favicon.ico"
        ); // 关键：在注册层面排除
    }
}
