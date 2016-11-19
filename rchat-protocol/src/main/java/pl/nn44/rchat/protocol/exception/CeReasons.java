package pl.nn44.rchat.protocol.exception;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates which ChatException.Reasons may be returned by given method.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface CeReasons {

    ChatException.Reason[] value();
}
