package pl.nn44.rchat.server.as;

import com.google.common.collect.ImmutableMap;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

@Aspect
public class AsLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AsLogger.class);

    private final Map<Integer, Function<Logger, AsPrinter>> loggers =
            new ImmutableMap.Builder<Integer, Function<Logger, AsPrinter>>()
                    .put(AsLoggable.NONE, (log) -> (f, a) -> {
                        // none!
                    })
                    .put(AsLoggable.TRACE, (log) -> log::trace)
                    .put(AsLoggable.DEBUG, (log) -> log::debug)
                    .put(AsLoggable.INFO, (log) -> log::info)
                    .put(AsLoggable.WARN, (log) -> log::warn)
                    .put(AsLoggable.ERROR, (log) -> log::error)
                    .build();

    @Around("(execution(* *(..)) && @annotation(AsLoggable))" // method
            + " ||" // or
            + " (execution(public * (@AsLoggable *).*(..))" // class
            + " && !execution(String *.toString())"
            + " && !execution(int *.hashCode())"
            + " && !execution(boolean *.equals(Object)))")
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
        loggers.get(annotation.level()).apply(logger).log(
                "#{}({}): {} ({}ms)",
                MethodSignature.class.cast(point.getSignature()).getMethod().getName(),
                removeFirstAndLastChar(Arrays.deepToString(point.getArgs())),
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
