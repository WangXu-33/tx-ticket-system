package com.rbac.base.core.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.rbac.base.core.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 代码生成/修改时间：2026-07-02
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
     * 代码生成/修改时间：2026-07-02
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
     * 代码生成/修改时间：2026-07-02
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
     * 代码生成/修改时间：2026-07-02
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
     * 代码生成/修改时间：2026-07-02
     * 功能说明：处理数据库访问异常，避免 SQL 细节和表结构信息暴露到前端。
     * 入参：数据库访问异常。
     * 出参：标准响应体。
     * 异常场景：数据库表缺失、SQL 执行失败、连接异常。
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<?> handlerDataAccessException(DataAccessException e) {
        log.error("数据库访问异常", e);
        return Result.error("数据库访问异常，请检查数据库初始化是否完整");
    }

    /**
     * 代码生成/修改时间：2026-07-05
     * 功能说明：处理 SSE 长连接断开后的 IO 异常，避免在 text/event-stream 响应上再次尝试回写 JSON 导致二次报错。
     * 入参：IO 异常、当前 HTTP 请求。
     * 出参：对 SSE 请求直接结束；其他 IO 异常返回标准错误响应。
     * 异常场景：浏览器刷新、页面切换或主动关闭时，SSE 连接可能抛出 IOException。
     */
    @ExceptionHandler(IOException.class)
    public Result<?> handlerIOException(IOException e, HttpServletRequest request) {
        if (isSseRequest(request)) {
            log.warn("SSE 连接已断开", e);
            return null;
        }
        log.error("IO 异常", e);
        return Result.error("系统响应异常，请稍后重试");
    }

    /**
     * 代码生成/修改时间：2026-07-05
     * 功能说明：处理兜底系统异常，并对 SSE 请求做特殊收口，避免流式响应被 JSON 异常处理再次污染。
     * 入参：未被其他处理器捕获的异常、当前 HTTP 请求。
     * 出参：标准响应体；SSE 请求返回空响应。
     * 异常场景：未知运行时异常、浏览器主动中止 SSE 流、异步写流异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handlerException(Exception e, HttpServletRequest request) {
        if (isSseRequest(request)) {
            log.warn("SSE 非 IO 异常", e);
            return null;
        }
        log.error("系统异常", e);
        return Result.error("系统异常，请联系管理员");
    }

    private boolean isSseRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accept = request.getHeader("Accept");
        String contentType = request.getContentType();
        String uri = request.getRequestURI();
        return (accept != null && accept.contains(MediaType.TEXT_EVENT_STREAM_VALUE))
                || (contentType != null && contentType.contains(MediaType.TEXT_EVENT_STREAM_VALUE))
                || (uri != null && uri.contains("/realtime/sse"));
    }
}
