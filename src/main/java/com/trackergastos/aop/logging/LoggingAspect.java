package com.trackergastos.aop.logging;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Aspect for logging execution of service and repository Spring components.
 *
 * By default, it only runs with the "dev" profile.
 */
@Aspect
public class LoggingAspect {

    private final Environment env;

    public LoggingAspect(Environment env) {
        this.env = env;
    }

    /**
     * Pointcut that matches all repositories, services and Web REST endpoints.
     */
    @Pointcut(
        """
        within(@org.springframework.stereotype.Repository *)
        || within(@org.springframework.stereotype.Service *)
        || within(@org.springframework.web.bind.annotation.RestController *)
        """
    )
    public void springBeanPointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advice methods.
    }

    /**
     * Pointcut that matches all Spring beans in the application's main packages.
     */
    @Pointcut(
        """
        within(com.trackergastos.repository..*)
        || within(com.trackergastos.service..*)
        || within(com.trackergastos.web.rest..*)
        """
    )
    public void applicationPackagePointcut() {
        // Method is empty as this is just a Pointcut, the implementations are in the advice methods.
    }

    /**
     * Retrieves the {@link Logger} associated to the given {@link JoinPoint}.
     *
     * @param joinPoint join point we want the logger for.
     * @return {@link Logger} associated to the given {@link JoinPoint}.
     */
    private Logger logger(JoinPoint joinPoint) {
        return LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
    }

    /**
     * Advice that logs methods throwing exceptions.
     *
     * @param joinPoint join point for advice.
     * @param e exception.
     */
    @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        logException(joinPoint, e);
    }

    /**
     * Advice that logs when a method is entered and exited.
     *
     * @param joinPoint join point for advice.
     * @return result.
     * @throws Throwable rethrows the exception thrown by the advised method.
     */
    @Around("applicationPackagePointcut() && springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        var log = logger(joinPoint);
        if (log.isDebugEnabled()) {
            // Spec 004: sin argumentos ni resultados (traerían montos, descripciones o emails)
            log.debug("Enter: {}()", joinPoint.getSignature().getName());
        }
        try {
            Object result = joinPoint.proceed();
            logExit(log, joinPoint, result);
            return result;
        } catch (IllegalArgumentException e) {
            logIllegalArgument(log, joinPoint);
            throw e;
        }
    }

    private void logExit(Logger log, JoinPoint joinPoint, Object result) {
        if (log.isDebugEnabled()) {
            log.debug("Exit: {}()", joinPoint.getSignature().getName());
        }
    }

    private void logIllegalArgument(Logger log, JoinPoint joinPoint) {
        if (log.isErrorEnabled()) {
            log.error("Illegal argument in {}()", joinPoint.getSignature().getName());
        }
    }

    private void logException(JoinPoint joinPoint, Throwable e) {
        // Spec 004: solo el tipo; el mensaje puede traer datos sensibles (ej. el email de un login)
        if (logger(joinPoint).isDebugEnabled()) {
            logger(joinPoint).debug("Exception in {}(): {}", joinPoint.getSignature().getName(), e.getClass().getName());
        }
    }
}
