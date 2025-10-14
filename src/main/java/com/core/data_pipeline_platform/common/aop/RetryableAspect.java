package com.core.data_pipeline_platform.common.aop;


import com.core.data_pipeline_platform.common.annotation.Retryable;
import jakarta.persistence.OptimisticLockException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RetryableAspect {

    @Around("@annotation(retryable)")
    public Object retryOnOptimisticLockFailure(ProceedingJoinPoint joinPoint, Retryable retryable) throws Throwable {
        int attempts = retryable.attempts();
        long delay = retryable.delay();
        Throwable lastException = null;

        for(int i = 0; i < attempts; i++) {
            try{
                return joinPoint.proceed();
            } catch (OptimisticLockException ole) {
                lastException = ole;
                Thread.sleep(delay);
            }
        }
        throw lastException;
    }
}
