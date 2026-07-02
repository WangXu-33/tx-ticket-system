package com.rbac.base.core.annotation;

import java.lang.annotation.*;

/**
 * 数据权限过滤注解
 * <p>
 * 用于标记需要进行数据范围过滤的方法。
 * 配合 DataScopeAspect 与 MyBatis-Plus DataPermissionHandler 实现 SQL 动态拦截拼接。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 部门表的别名（在SQL中关联时的别名，如 d）
     */
    String deptAlias() default "";

    /**
     * 用户表的别名（在SQL中关联时的别名，如 u）
     */
    String userAlias() default "";

    /**
     * 部门字段名
     */
    String deptColumn() default "dept_id";

    /**
     * 用户字段名
     */
    String userColumn() default "create_by";
}
