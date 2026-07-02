package com.rbac.base.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.rbac.base.core.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 修改时间：2026-07-02
     * 功能说明：处理未登录异常，返回统一认证失败响应。
     * 入参：Sa-Token 未登录异常。
     * 出参：标准响应体。
     * 异常场景：无。
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<?> handlerNotLoginException(NotLoginException nle) {
        return Result.error(401, "未登录，请先登录");
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：处理权限不足异常，返回统一授权失败响应。
     * 入参：Sa-Token 权限异常。
     * 出参：标准响应体。
     * 异常场景：无。
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<?> handlerNotPermissionException(NotPermissionException npe) {
        return Result.error(403, "没有该权限：" + npe.getPermission());
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：处理角色不足异常，返回统一授权失败响应。
     * 入参：Sa-Token 角色异常。
     * 出参：标准响应体。
     * 异常场景：无。
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<?> handlerNotRoleException(NotRoleException nre) {
        return Result.error(403, "没有该角色：" + nre.getRole());
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：处理业务参数异常，保留明确的参数校验提示。
     * 入参：非法参数异常。
     * 出参：标准响应体。
     * 异常场景：无。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<?> handlerIllegalArgumentException(IllegalArgumentException e) {
        return Result.error(e.getMessage());
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：处理数据库访问异常，避免 SQL 细节和表结构信息暴露到前端。
     * 入参：数据库访问异常。
     * 出参：标准响应体。
     * 异常场景：数据库表缺失、SQL 执行失败、连接异常。
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<?> handlerDataAccessException(DataAccessException e) {
        log.error("数据访问异常", e);
        return Result.error("数据访问异常，请检查数据库初始化是否完整");
    }

    /**
     * 修改时间：2026-07-02
     * 功能说明：处理兜底系统异常，后端记录详细日志，前端展示稳定提示。
     * 入参：未被其他处理器捕获的异常。
     * 出参：标准响应体。
     * 异常场景：未知运行时异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handlerException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常，请联系管理员");
    }
}
