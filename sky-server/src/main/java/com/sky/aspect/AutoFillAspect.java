package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/** 在 Mapper 执行 SQL 前，统一填充创建/修改时间与操作人。 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    @Pointcut("execution(* com.sky.mapper..*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() { }

    @Before(value = "autoFillPointCut() && @annotation(autoFill)", argNames = "joinPoint,autoFill")
    public void autoFill(JoinPoint joinPoint, AutoFill autoFill) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || args[0] == null) {
            throw new IllegalArgumentException("@AutoFill 方法的第一个参数必须是实体对象");
        }
        Long currentId = BaseContext.getCurrentId();
        if (currentId == null) {
            throw new IllegalStateException("自动填充公共字段前必须设置当前操作人");
        }
        Object entity = args[0];
        LocalDateTime now = LocalDateTime.now();
        try {
            if (autoFill.value() == OperationType.INSERT) {
                set(entity, AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class, now);
                set(entity, AutoFillConstant.SET_CREATE_USER, Long.class, currentId);
            }
            set(entity, AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class, now);
            set(entity, AutoFillConstant.SET_UPDATE_USER, Long.class, currentId);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("自动填充失败：实体必须提供对应公共字段的 setter 方法", ex);
        }
        log.debug("公共字段自动填充：{} {}", autoFill.value(), joinPoint.getSignature().toShortString());
    }

    private void set(Object entity, String method, Class<?> type, Object value)
            throws ReflectiveOperationException {
        entity.getClass().getMethod(method, type).invoke(entity, value);
    }
}
