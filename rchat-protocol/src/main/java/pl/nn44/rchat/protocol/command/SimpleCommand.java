package pl.nn44.rchat.protocol.command;

import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.model.Response;

/**
 * Functional interface for "simple" commands.<br/>
 * It is to provide common interface and make processing easier.
 */
@FunctionalInterface
public interface SimpleCommand {

    Response<?> accept(String session, String param1, String param2) throws ChatException;
}
