package pl.nn44.rchat.protocol;

import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.model.Response;

public interface StatefulCommand {

    Response<?> accept(String session, String channel, String param, boolean state) throws ChatException;
}
