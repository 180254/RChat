package pl.nn44.rchat.server.as;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AsLoggable {

    int NONE = 0;
    int TRACE = 1;
    int DEBUG = 2;
    int INFO = 3;
    int WARN = 4;
    int ERROR = 5;

    int level() default AsLoggable.INFO;

    boolean result() default false;
}
