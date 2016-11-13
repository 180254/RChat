package pl.nn44.rchat.client.util;

import pl.nn44.rchat.protocol.ChatException;
import pl.nn44.rchat.protocol.Response;

public interface StateCommand {

    Response<?> cmd(String session, String channel, String param, boolean state) throws ChatException;
}