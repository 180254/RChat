package pl.nn44.rchat.protocol;

import pl.nn44.rchat.protocol.exception.CeReasons;
import pl.nn44.rchat.protocol.exception.ChatException;
import pl.nn44.rchat.protocol.model.Channel;
import pl.nn44.rchat.protocol.model.Response;
import pl.nn44.rchat.protocol.model.WhatsUp;
import pl.nn44.rchat.protocol.model.WuFeedback;

import javax.annotation.Nullable;

import static pl.nn44.rchat.protocol.exception.ChatException.Reason.*;

public interface ChatService {

    @CeReasons({ALREADY_LOGGED_IN, GIVEN_BAD_USERNAME, GIVEN_BAD_PASSWORD})
    @WuFeedback(true /*NOTHING*/)
    Response<String> login(String username, @Nullable String password) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION})
    @WuFeedback(true /*NOTHING*/)
    Response<?> logout(String session) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION})
    @WuFeedback(false)
    Response<Channel[]> channels(String session) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_PASSWORD, UNWELCOME_BANNED})
    @WuFeedback(false)
    Response<Channel> join(String session, String channel, @Nullable String password) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL})
    @WuFeedback(false)
    Response<?> part(String session, String channel, String unused) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, NO_PERMISSION})
    @WuFeedback(true)
    Response<?> topic(String session, String channel, String text) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    @WuFeedback(true)
    Response<?> kick(String session, String channel, String username) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    @WuFeedback(true)
    Response<?> ban(String session, String channel, String username, boolean state) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    @WuFeedback(true)
    Response<?> admin(String session, String channel, String username, boolean state) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_USERNAME, NO_PERMISSION})
    @WuFeedback(true)
    Response<?> ignore(String session, String unused, String username, boolean state) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, NO_PERMISSION})
    @WuFeedback(true)
    Response<?> message(String session, String channel, String text) throws ChatException;

    @CeReasons({GIVEN_BAD_SESSION, GIVEN_BAD_CHANNEL, GIVEN_BAD_USERNAME, NO_PERMISSION})
    @WuFeedback(true)
    Response<?> privy(String session, String username, String text) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({GIVEN_BAD_SESSION})
    @WuFeedback(false)
    Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws ChatException;

    // ---------------------------------------------------------------------------------------------------------------

    @CeReasons({NO_PERMISSION})
    @WuFeedback(false)
    Response<?> test(boolean exception) throws ChatException;
}
