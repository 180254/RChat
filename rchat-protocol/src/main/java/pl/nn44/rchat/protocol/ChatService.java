package pl.nn44.rchat.protocol;

import org.jetbrains.annotations.Nullable;

public interface ChatService {

    Response<String> login(String username, @Nullable String password) throws ChatException;

    Response<?> logout(String session) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    Response<RcChannel[]> channels(String session) throws ChatException;

    Response<RcChannel> join(String session, String channel, @Nullable String password) throws ChatException;

    Response<?> part(String session, String channel) throws ChatException;

    Response<?> topic(String session, String channel, String text) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    Response<?> kick(String session, String channel, String username) throws ChatException;

    Response<?> ban(String session, String channel, String username, boolean state) throws ChatException;

    Response<?> admin(String session, String channel, String username, boolean state) throws ChatException;

    Response<?> ignore(String session, String username, boolean state) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    Response<?> privy(String session, String username, String text) throws ChatException;

    Response<?> message(String session, String channel, String text) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    Response<?> test(boolean exception) throws ChatException;
}
