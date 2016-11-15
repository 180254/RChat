package pl.nn44.rchat.server.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import pl.nn44.rchat.protocol.model.WhatsUp;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerUser {

    private final String session;
    private final String username;
    private LocalDateTime lastSync;

    private final CopyOnWriteArrayList<ServerChannel> channels = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ServerUser> ignored = new CopyOnWriteArrayList<>();
    private final BlockingQueue<WhatsUp> news = new LinkedBlockingQueue<>(); // offer(e), poll(), peek()

    public ServerUser(String session, String username) {
        this.session = session;
        this.username = username;
        this.lastSync = LocalDateTime.now();
    }

    public static ServerUser dummyUser(String username) {
        return new ServerUser(null, username);
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getSession() {
        return session;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getLastSync() {
        return lastSync;
    }

    public CopyOnWriteArrayList<ServerChannel> getChannels() {
        return channels;
    }

    public CopyOnWriteArrayList<ServerUser> getIgnored() {
        return ignored;
    }

    public BlockingQueue<WhatsUp> getNews() {
        return news;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public void updateLastSync() {
        this.lastSync = LocalDateTime.now();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerUser user = (ServerUser) o;
        return Objects.equal(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("session", session)
                .add("username", username)
                .add("lastSync", lastSync)
                .add("channels", channels.stream().map(ServerChannel::getName).toArray())
                .add("ignored", ignored.stream().map(ServerUser::getUsername).toArray())
                // .add("news", news)
                .toString();
    }
}
