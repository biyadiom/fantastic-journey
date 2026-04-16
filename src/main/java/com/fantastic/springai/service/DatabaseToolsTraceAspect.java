package com.fantastic.springai.service;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Enregistre chaque appel de méthode sur {@link DatabaseToolsService} (outils JPA / JPQL).
 */
@Aspect
@Component
public class DatabaseToolsTraceAspect {

    @Around("execution(* com.fantastic.springai.service.DatabaseToolsService.*(..))")
    public Object record(ProceedingJoinPoint pjp) throws Throwable {
        String sig = pjp.getSignature().getName();
        String args = formatArgs(pjp.getArgs());
        ToolInvocationTraceHolder.add("DatabaseToolsService." + sig + "(" + args + ")");
        return pjp.proceed();
    }

    private static String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(argToString(args[i]));
        }
        return b.toString();
    }

    private static String argToString(Object o) {
        if (o == null) {
            return "null";
        }
        String s = String.valueOf(o);
        if (s.length() > 240) {
            return s.substring(0, 237) + "...";
        }
        return s;
    }
}
