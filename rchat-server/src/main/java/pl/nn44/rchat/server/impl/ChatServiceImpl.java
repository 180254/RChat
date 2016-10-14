package pl.nn44.rchat.server.impl;

import pl.nn44.rchat.protocol.*;

public class ChatServiceImpl implements ChatService {

    private static final long serialVersionUID = -4849634603399107637L;

    int x = 0;


    @Override
    public Response<String> login(String username, String password) {
        return null;
    }

    @Override
    public Response join(String session, String channel, String password) {
        return null;
    }

    @Override
    public Response part(String session, String channel) {
        return null;
    }

    @Override
    public Response kick(String session, String channel, String username) {
        return null;
    }

    @Override
    public Response ban(String session, String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response<ChannelUser[]> names(String session, String channel) {
        return null;
    }

    @Override
    public Response topic(String session, String channel) {
        return null;
    }

    @Override
    public Response topic(String session, String channel, String text) {
        return null;
    }

    @Override
    public Response admin(String session, String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response ignore(String session, String channel, String username, boolean state) {
        return null;
    }

    @Override
    public Response privy(String session, String nickname, String text) {
        return null;
    }

    @Override
    public Response message(String session, String channel, String message) {
        x++;// ensure same instance
        return new Response<>(Status.OK, x + "/" + session + "/" + channel + "/" + message);
    }

    @Override
    public Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs) {
        try {
            Thread.sleep(longPoolingTimeoutMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new Response<>(Status.OK, new WhatsUp[0]);
    }
}
