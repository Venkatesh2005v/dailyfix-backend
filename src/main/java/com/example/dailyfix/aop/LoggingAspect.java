package com.example.dailyfix.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingAspect.class);


    @Before("execution(* com.example.dailyfix.controller..*(..))")
    public void logControllerEntry(JoinPoint joinPoint) {
        logger.info(
                "Controller called: {}()",
                joinPoint.getSignature().toShortString()
        );
    }


    @Before("execution(* com.example.dailyfix.service..*(..))")
    public void logServiceEntry(JoinPoint joinPoint) {
        logger.info(
                "Service method start: {}()",
                joinPoint.getSignature().toShortString()
        );
    }

    @AfterReturning(
            pointcut = "execution(* com.example.dailyfix.service..*(..))",
            returning = "result"
    )
    public void logServiceExit(JoinPoint joinPoint, Object result) {
        logger.info(
                "Service method completed: {}()",
                joinPoint.getSignature().toShortString()
        );
    }

    @AfterThrowing(
            pointcut =
                    "execution(* com.example.dailyfix.controller..*(..)) || " +
                            "execution(* com.example.dailyfix.service..*(..))",
            throwing = "ex"
    )
    public void logException(JoinPoint joinPoint, Exception ex) {
        logger.error(
                "Exception in {}() : {}",
                joinPoint.getSignature().toShortString(),
                ex.getMessage()
        );
    }
}
