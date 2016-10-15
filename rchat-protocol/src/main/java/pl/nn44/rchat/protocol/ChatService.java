package pl.nn44.rchat.protocol;

import org.jetbrains.annotations.Nullable;

public interface ChatService {

    Response<String> login(String username, @Nullable String password) throws RChatException;

    Response logout(String session) throws RChatException;

    Response join(String session, String channel, @Nullable String password) throws RChatException;

    Response part(String session, String channel) throws RChatException;

    Response kick(String session, String channel, String username) throws RChatException;

    Response ban(String session, String channel, String username, boolean state) throws RChatException;

    Response<ChannelUser[]> names(String session, String channel) throws RChatException;

    Response topic(String session, String channel) throws RChatException;

    Response topic(String session, String channel, String text) throws RChatException;

    Response admin(String session, String channel, String username, boolean state) throws RChatException;

    Response ignore(String session, String channel, String username, boolean state) throws RChatException;

    Response privy(String session, String nickname, String text) throws RChatException;

    Response message(String session, String channel, String message) throws RChatException;

    Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws RChatException;
}
