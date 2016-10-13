package pl.nn44.rchat.server.impl;

import pl.nn44.rchat.protocol.*;

public class ChatServiceImpl implements ChatService {

    private static final long serialVersionUID = -4849634603399107637L;

    @Override
    public Response login(String username, String password) {
        return null;
    }

    @Override
    public Response join(String channel, String password) {
        return null;
    }

    @Override
    public Response part(String channel) {
        return null;
    }

    @Override
    public Response kick(String channel, String username) {
        return null;
    }

    @Override
    public Response ban(String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response<ChannelUser[]> names(String channel) {
        return null;
    }

    @Override
    public Response topic(String channel) {
        return null;
    }

    @Override
    public Response topic(String channel, String text) {
        return null;
    }

    @Override
    public Response admin(String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response ignore(String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response privy(String nickname, String text) {
        return null;
    }

    @Override
    public Response message(String channel, String message) {
        return new Response<>(Status.OK, "none");
    }

    @Override
    public Response<WhatsUp[]> whatsUp(int longPoolingTimeoutMs) {
        try {
            Thread.sleep(longPoolingTimeoutMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new Response<>(Status.OK, new WhatsUp[0]);
    }
}
