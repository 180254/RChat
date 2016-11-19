package pl.nn44.rchat.protocol.command;

import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.model.Response;

@FunctionalInterface
public interface SimpleCommand {

    Response<?> accept(String session, String channel, String param) throws ChatException;
}