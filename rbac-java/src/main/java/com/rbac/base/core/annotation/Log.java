package com.rbac.base.core.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /** 模块标题 */
    String title() default "";
    
    /** 业务类型（0其它 1新增 2修改 3删除） */
    int businessType() default 0;
}
