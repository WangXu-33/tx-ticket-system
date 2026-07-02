package com.rbac.base.core.aspect;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbac.base.core.annotation.Log;
import com.rbac.base.modules.log.entity.SysOperLog;
import com.rbac.base.modules.log.mapper.SysOperLogMapper;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
public class LogAspect {

    @Autowired
    private SysOperLogMapper operLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    // 配置织入点
    @Pointcut("@annotation(com.rbac.base.core.annotation.Log)")
    public void logPointCut() {}

    /**
     * 处理完请求后执行
     */
    @AfterReturning(pointcut = "logPointCut()", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Object jsonResult) {
        handleLog(joinPoint, null, jsonResult);
    }

    /**
     * 拦截异常操作
     */
    @AfterThrowing(value = "logPointCut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, final Exception e, Object jsonResult) {
        try {
            // 获得注解
            Log controllerLog = getAnnotationLog(joinPoint);
            if (controllerLog == null) {
                return;
            }

            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(0);
            
            // 请求的地址
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operLog.setOperIp(getClientIp(request));
                operLog.setOperUrl(StrUtil.sub(request.getRequestURI(), 0, 255));
                operLog.setRequestMethod(request.getMethod());
            }

            if (e != null) {
                operLog.setStatus(1);
                operLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 2000));
            }
            
            // 设置方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            
            // 设置操作人类别
            try {
                if (StpUtil.isLogin()) {
                    // 由于没有存username到Token，假设我们只存了ID或者通过其它方式获取。
                    // 简单起见记录登录ID
                    operLog.setOperName(StpUtil.getLoginIdAsString());
                } else {
                    operLog.setOperName("未登录");
                }
            } catch (Exception ex) {
                operLog.setOperName("异常");
            }
            
            // 处理设置注解上的参数
            operLog.setTitle(controllerLog.title());
            operLog.setBusinessType(controllerLog.businessType());
            
            // 设置参数
            if (joinPoint.getArgs() != null && joinPoint.getArgs().length > 0) {
                String params = objectMapper.writeValueAsString(joinPoint.getArgs()[0]);
                operLog.setOperParam(StrUtil.sub(params, 0, 2000));
            }
            
            if (jsonResult != null) {
                operLog.setJsonResult(StrUtil.sub(objectMapper.writeValueAsString(jsonResult), 0, 2000));
            }
            
            operLog.setOperTime(LocalDateTime.now());
            // 插入数据
            operLogMapper.insert(operLog);
            
        } catch (Exception exp) {
            exp.printStackTrace();
        }
    }

    /**
     * 是否存在注解，如果存在就获取
     */
    private Log getAnnotationLog(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getMethod().getAnnotation(Log.class);
    }
    
    /**
     * 简单的 IP 获取
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
