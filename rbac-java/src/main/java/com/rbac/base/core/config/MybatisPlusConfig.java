package com.rbac.base.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.toolkit.JdbcUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class MybatisPlusConfig {

    @Autowired
    private CustomDataPermissionHandler dataPermissionHandler;

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    /**
     * 添加插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加数据权限插件
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataPermissionHandler));
        // 如果配置多个插件, 切记分页最后添加
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor();
        if (StringUtils.hasText(jdbcUrl)) {
            DbType dbType = JdbcUtils.getDbType(jdbcUrl);
            if (dbType != null) {
                paginationInnerInterceptor.setDbType(dbType);
            }
        }
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
