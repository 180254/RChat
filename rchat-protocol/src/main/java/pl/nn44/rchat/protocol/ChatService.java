package pl.nn44.rchat.protocol;

import org.jetbrains.annotations.Nullable;
import pl.nn44.rchat.protocol.exception.CeReasons;
import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.model.Channel;
import pl.nn44.rchat.protocol.model.Response;
import pl.nn44.rchat.protocol.model.WhatsUp;

import static pl.nn44.rchat.protocol.exception.ChatException.Reason.*;

public interface ChatService {

    @CeReasons({ALREADY_LOGGED_IN, GIVEN_BAD_USERNAME, GIVEN_BAD_PASSWORD})
    Response<String> login(String username, @Nullable String password) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION})
    Response<?> logout(String session) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION})
    Response<Channel[]> channels(String session) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_PASSWORD, UNWELCOME_BANNED})
    Response<Channel> join(String session, String channel, @Nullable String password) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL})
    Response<?> part(String session, String channel, String unused) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, NO_PERMISSION})
    Response<?> topic(String session, String channel, String text) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    Response<?> kick(String session, String channel, String username) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    Response<?> ban(String session, String channel, String username, boolean state) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    Response<?> admin(String session, String channel, String username, boolean state) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_USERNAME, NO_PERMISSION})
    Response<?> ignore(String session, String unused, String username, boolean state) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    Response<?> privy(String session, String username, String text) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, NO_PERMISSION})
    Response<?> message(String session, String channel, String text) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION})
    Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({NO_PERMISSION})
    Response<?> test(boolean exception) throws ChatException;
}
