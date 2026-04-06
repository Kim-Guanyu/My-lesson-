package com.mdkj.component;// ... existing code ...

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.mdkj.exception.ServiceException;
import com.mdkj.util.ResultCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Slf4j
@Aspect
// @Component  // 移除此注解，由各微服务模块自行决定是否启用
public class ServiceAspect {

    // Deleted:@Around("execution(public * com.mdkj.service.impl.*.*(..))")
    @Around("within(@org.springframework.stereotype.Service *) && execution(* *(..))")
    @SneakyThrows
    public Object aroundAdvice(ProceedingJoinPoint pjp) {

        // 获取方法参数
        Object[] args = pjp.getArgs();
        // 组装完整业务方法名：类名.方法名 ()
        String className = pjp.getTarget().toString();
        className = className.substring(className.lastIndexOf('.') + 1, className.indexOf('@'));
        String methodName = pjp.getSignature().getName();
        methodName = className + "." + methodName + "()";

        // 前置通知：检查参数中是否存在空值
        if (ObjectUtil.hasNull(args)) {
            throw new ServiceException(ResultCode.ILLEGAL_PARAM, "业务方法 " + methodName + "中存在 null 值参数");
        }

        // 调用目标方法
        Object returnValue = pjp.proceed(args);

        // 后置通知：记录业务层调用日志
        log.info(String.format("""
                        ========== 业务日志 ==========
                        业务方法：%s
                        业务参数：%s
                        响应数据：%s
                        """,
                methodName, JSONUtil.toJsonStr(args), JSONUtil.toJsonStr(returnValue)));

        // 返回目标方法的返回值
        return returnValue;
    }
}
