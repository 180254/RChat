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

@Aspect
public class AsLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AsLogger.class);

    private final Map<Integer, AsPrinter> loggers =
            new ImmutableMap.Builder<Integer, AsPrinter>()
                    .put(AsLoggable.NONE, ((f, a) -> {
                        // none!
                    }))
                    .put(AsLoggable.TRACE, (LOG::trace))
                    .put(AsLoggable.DEBUG, (LOG::debug))
                    .put(AsLoggable.INFO, (LOG::info))
                    .put(AsLoggable.WARN, (LOG::warn))
                    .put(AsLoggable.ERROR, (LOG::error))
                    .build();

    @Around("(execution(* *(..)) && @annotation(AsLoggable))" // method
            + "|| execution(public * (@AsLoggable *).*(..))" // class
            + " && !execution(String *.toString())"
            + " && !execution(int *.hashCode())"
            + " && !execution(boolean *.equals(Object))")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Method method = MethodSignature.class.cast(point.getSignature()).getMethod();

        AsLoggable annotation = method.getAnnotation(AsLoggable.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(AsLoggable.class);
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

        loggers.get(annotation.level()).log(
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
