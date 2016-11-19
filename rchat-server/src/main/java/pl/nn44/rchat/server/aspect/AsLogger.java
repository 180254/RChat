package pl.nn44.rchat.server.aspect;

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
import java.util.Objects;
import java.util.function.Function;

@Aspect
public class AsLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AsLogger.class);

    private final Map<Level, Function<Logger, Printer>> loggers =
            new ImmutableMap.Builder<Level, Function<Logger, Printer>>()
                    .put(Level.TRACE, (log) -> log::trace)
                    .put(Level.DEBUG, (log) -> log::debug)
                    .put(Level.INFO, (log) -> log::info)
                    .put(Level.WARN, (log) -> log::warn)
                    .put(Level.ERROR, (log) -> log::error)
                    .build();

    // @formatter:off
    @Around(
            "" +
                    /* class */
                    "(" +
                        "execution(public * (@pl.nn44.rchat.server.aspect.Loggable *).*(..))" +
                        " && !execution(String *.toString())" +
                        " && !execution(int *.hashCode())" +
                        " && !execution(boolean *.equals(Object))" +
                    ")" +
                    "||" +
                    /* method*/
                    "(" +
                        "execution(* *(..))" +
                        " && @annotation(pl.nn44.rchat.server.aspect.Loggable)" +
                    ")"
    )
    // @formatter:on
    public Object around(ProceedingJoinPoint point) throws Throwable {
        LOG.trace("AROUND: {}", point);

        Method method = MethodSignature.class.cast(point.getSignature()).getMethod();
        Class<?> clazz = method.getDeclaringClass();

        Loggable annotation = method.getAnnotation(Loggable.class);
        if (annotation == null) {
            annotation = clazz.getAnnotation(Loggable.class);
        }

        long startMs = System.currentTimeMillis();
        Object result = null;
        Throwable throwable = null;
        try {
            result = point.proceed();
        } catch (Throwable ex) {
            throwable = ex;
        }
        long timeMs = System.currentTimeMillis() - startMs;

        Logger logger = LoggerFactory.getLogger(clazz);
        Printer printer = loggers.get(annotation.level()).apply(logger);
        printer.log(
                "#{}({}): {} ({}ms)",
                MethodSignature.class.cast(point.getSignature()).getMethod().getName(),
                annotation.params() ? arrayToString(point.getArgs()) : "_",
                annotation.result() ? firstNonNull(throwable, result) : "_",
                timeMs
        );

        if (throwable != null) {
            throw throwable;
        }
        return result;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String arrayToString(Object[] array) {
        String s = Arrays.deepToString(array);
        return s.substring(1, s.length() - 1);
    }

    @SafeVarargs
    public static <T> T firstNonNull(T... items) {
        return Arrays.stream(items)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
