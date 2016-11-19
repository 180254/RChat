package pl.nn44.rchat.protocol.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates if given method generates "accept confirmation"<br/>
 * and send to caller the WhatsUp notification related to that action.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD})
public @interface WuFeedback {

    boolean value();
}
