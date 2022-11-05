package com.zhao.aspect;


import com.alibaba.fastjson.JSON;
import com.zhao.annotations.OptLog;
import com.zhao.mapper.OperationLogMapper;
import com.zhao.pojo.OperationLog;
import com.zhao.utils.IpUtil;
import com.zhao.utils.UserUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * 操作日志切面处理
 *
 * @author ran-feiran
 * @date 2022/10/17
 */
@Component
@Aspect
@Slf4j
public class OptLogAspect {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 设置操作日志切入点 记录操作日志 在注解的位置切入代码
     */
    @Pointcut("@annotation(com.zhao.annotations.OptLog)")
    public void optLogPointCut() {}

    @AfterReturning(value = "optLogPointCut()", returning = "keys")
    @SuppressWarnings("all")
    public void saveOptLog(JoinPoint joinPoint, Object keys) {
        log.info("===========日志切面开始执行===========");
        // 获取RequestAttributes
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 从获取RequestAttributes中获取HttpServletRequest的信息
        HttpServletRequest request = (HttpServletRequest) Objects.requireNonNull(requestAttributes).resolveReference(RequestAttributes.REFERENCE_REQUEST);
        OperationLog operationLog = new OperationLog();
        // 从切面织入点处通过反射机制获取织入点处的方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取切入点所在的方法
        Method method = signature.getMethod();
        // 获取操作 分别拿到三个注解 Api ApiOperation OptLog
        Api api = (Api) signature.getDeclaringType().getAnnotation(Api.class);
        ApiOperation apiOperation = method.getAnnotation(ApiOperation.class);
        OptLog optLog = method.getAnnotation(OptLog.class);
        // 操作模块
        operationLog.setOptModule(api.tags()[0]);
        // 操作类型
        operationLog.setOptType(optLog.optType());
        // 操作描述
        operationLog.setOptDesc(apiOperation.value());
        // 获取请求的类名
        String className = joinPoint.getTarget().getClass().getName();
        // 获取请求的方法名
        String methodName = method.getName();
        methodName = className + "." + methodName;
        // 请求方式
        operationLog.setRequestMethod(Objects.requireNonNull(request).getMethod());
        // 请求方法
        operationLog.setOptMethod(methodName);
        // 请求参数
        operationLog.setRequestParam(JSON.toJSONString(joinPoint.getArgs()));
        // 返回结果
        operationLog.setResponseData(JSON.toJSONString(keys));
        // 请求用户ID
        operationLog.setUserId(UserUtil.getLoginUser().getUserId());
        // 请求用户
        operationLog.setNickname(UserUtil.getLoginUser().getNickname());
        // 请求IP
        String ipAddress = IpUtil.getIp(request);
        operationLog.setIpAddress(ipAddress);
        operationLog.setIpSource(IpUtil.getIpSource(ipAddress));
        // 请求URL
        operationLog.setOptUrl(request.getRequestURI());
        operationLogMapper.insert(operationLog);
        log.info("===========日志切面执行完毕===========");
    }

}
