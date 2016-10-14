package pl.nn44.rchat.protocol;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public interface ChatService extends Serializable {

    Response<String> login(String username, @Nullable String password);

    Response join(String session, String channel, @Nullable String password);

    Response part(String session, String channel);

    Response kick(String session, String channel, String username);

    Response ban(String session, String channel, String username, boolean state);

    Response<ChannelUser[]> names(String session, String channel);

    Response topic(String session, String channel);

    Response topic(String session, String channel, String text);

    Response admin(String session, String channel, String username, boolean state);

    Response ignore(String session, String channel, String username, boolean state);

    Response privy(String session, String nickname, String text);

    Response message(String session, String channel, String message);

    Response<WhatsUp[]> whatsUp(String session, int longPoolingTimeoutMs);
}
