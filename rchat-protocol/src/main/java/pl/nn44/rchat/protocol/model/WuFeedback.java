package pl.nn44.rchat.protocol.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <pre>
 * Indicates if given method generates "accept confirmation"
 * and send to caller WhatsUp notification related to that action.
 * </pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface WuFeedback {

    boolean value();
}
