package com.rbac.base.core.config;

/**
 * 线程局部变量，用于在同一线程的上下文中传递动态数据权限的 SQL 片段
 * <p>
 * 调用链为：Controller/Service 方法上的 @DataScope -> DataScopeAspect 写入 -> MyBatis-Plus DataPermissionHandler 读取。
 */
public class DataScopeContextHolder {

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void setSqlFilter(String sql) {
        CONTEXT_HOLDER.set(sql);
    }

    public static String getSqlFilter() {
        return CONTEXT_HOLDER.get();
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }
}
