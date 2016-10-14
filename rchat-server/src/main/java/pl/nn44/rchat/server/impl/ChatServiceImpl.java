package pl.nn44.rchat.server.impl;

import org.jetbrains.annotations.Nullable;
import pl.nn44.rchat.protocol.*;

public class ChatServiceImpl implements ChatService {

    @Override
    public Response<String> login(String username, @Nullable String password) throws RChatException {
        return null;
    }

    @Override
    public Response join(String session, String channel, @Nullable String password) throws RChatException {
        return null;
    }

    @Override
    public Response part(String session, String channel) throws RChatException {
        return null;
    }

    @Override
    public Response kick(String session, String channel, String username) throws RChatException {
        return null;
    }

    @Override
    public Response ban(String session, String channel, String username, boolean state) throws RChatException {
        return null;
    }

    @Override
    public Response<ChannelUser[]> names(String session, String channel) throws RChatException {
        return null;
    }

    @Override
    public Response topic(String session, String channel) throws RChatException {
        return null;
    }

    @Override
    public Response topic(String session, String channel, String text) throws RChatException {
        return null;
    }

    @Override
    public Response admin(String session, String channel, String username, boolean state) throws RChatException {
        return null;
    }

    @Override
    public Response ignore(String session, String channel, String username, boolean state) throws RChatException {
        return null;
    }

    @Override
    public Response privy(String session, String nickname, String text) throws RChatException {
        throw new RChatException(RChatException.Reason.BAD_PASSWORD);
    }

    @Override
    public Response message(String session, String channel, String message) throws RChatException {
        return null;
    }

    @Override
    public Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) throws RChatException {
        return new Response<>(new WhatsUp[0]);
    }
}
