package pl.nn44.rchat.protocol;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public interface ChatService extends Serializable {

    Response login(String username, @Nullable String password);

    Response join(String channel, @Nullable String password);

    Response part(String channel);

    Response kick(String channel, String username);

    Response ban(String channel, String username, boolean state);

    Response<ChannelUser[]> names(String channel);

    Response topic(String channel);

    Response topic(String channel, String text);

    Response admin(String channel, String username, boolean state);

    Response ignore(String channel, String username, boolean state);

    Response privy(String nickname, String text);

    Response message(String channel, String message);

    Response<WhatsUp[]> whatsUp(int longPoolingTimeoutMs);
}
