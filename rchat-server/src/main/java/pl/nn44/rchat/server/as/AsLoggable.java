package pl.nn44.rchat.server.as;

import org.slf4j.event.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AsLoggable {

    Level level() default Level.INFO;

    boolean params() default true;

    boolean result() default false;
}
