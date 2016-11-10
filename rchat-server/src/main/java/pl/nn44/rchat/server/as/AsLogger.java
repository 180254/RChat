package pl.nn44.rchat.server.as;

import com.google.common.collect.ImmutableMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

@Aspect
public class AsLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AsLogger.class);

    private final Map<Level, Function<Logger, AsPrinter>> loggers =
            new ImmutableMap.Builder<Level, Function<Logger, AsPrinter>>()
                    .put(Level.TRACE, (log) -> log::trace)
                    .put(Level.DEBUG, (log) -> log::debug)
                    .put(Level.INFO, (log) -> log::info)
                    .put(Level.WARN, (log) -> log::warn)
                    .put(Level.ERROR, (log) -> log::error)
                    .build();

    @Around("execution(public * (@AsLoggable *).*(..))"
            + " && !execution(String *.toString())"
            + " && !execution(int *.hashCode())"
            + " && !execution(boolean *.equals(Object))")
    public Object aClass(ProceedingJoinPoint point) throws Throwable {
        return around(point);
    }

    @Around("execution(* *(..)) && @annotation(AsLoggable))")
    public Object aMethod(ProceedingJoinPoint point) throws Throwable {
        return around(point);
    }

    public Object around(ProceedingJoinPoint point) throws Throwable {
        Method method = MethodSignature.class.cast(point.getSignature()).getMethod();
        Class<?> clazz = method.getDeclaringClass();

        AsLoggable annotation = method.getAnnotation(AsLoggable.class);
        if (annotation == null) {
            annotation = clazz.getAnnotation(AsLoggable.class);
        }

        long start = System.currentTimeMillis();
        Object result = null;
        Throwable throwable = null;
        try {
            result = point.proceed();
        } catch (Throwable ex) {
            throwable = ex;
        }
        long time = System.currentTimeMillis() - start;

        Logger logger = LoggerFactory.getLogger(clazz);
        AsPrinter printer = loggers.get(annotation.level()).apply(logger);
        printer.log(
                "#{}({}): {} ({}ms)",
                MethodSignature.class.cast(point.getSignature()).getMethod().getName(),
                annotation.params() ? removeFirstAndLastChar(Arrays.deepToString(point.getArgs())) : "_",
                annotation.result() ? (throwable == null ? result : throwable) : "_",
                time
        );

        if (throwable != null) {
            throw throwable;
        }
        return result;
    }

    public String removeFirstAndLastChar(String s) {
        return s.substring(1, s.length() - 1);
    }
}
