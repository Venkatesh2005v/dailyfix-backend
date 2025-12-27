package com.example.dailyfix.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ValidationAspect {

    @Before("execution(* com.example.dailyfix.service..*(..))")
    public void validateInputs(JoinPoint joinPoint) {

        Object[] args = joinPoint.getArgs();

        for (Object arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException(
                        "Invalid input: null value passed to " +
                                joinPoint.getSignature().toShortString()
                );
            }
        }
    }
}
