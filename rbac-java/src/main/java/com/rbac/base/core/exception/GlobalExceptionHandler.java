package com.rbac.base.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.rbac.base.core.common.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获：未登录异常
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handlerNotLoginException(NotLoginException nle) {
        return Result.error(401, "未登录，请先登录");
    }

    // 捕获：无权限异常
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handlerNotPermissionException(NotPermissionException npe) {
        return Result.error(403, "没有该权限：" + npe.getPermission());
    }

    // 捕获：无角色异常
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handlerNotRoleException(NotRoleException nre) {
        return Result.error(403, "没有该角色：" + nre.getRole());
    }

    // 捕获：所有其他异常
    @ExceptionHandler(Exception.class)
    public Result<?> handlerException(Exception e) {
        e.printStackTrace();
        return Result.error(e.getMessage());
    }
}
