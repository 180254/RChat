package pl.nn44.rchat.server.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import pl.nn44.rchat.protocol.WhatsUp;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class User {

    private final String session;
    private final String username;
    private LocalDateTime lastSync;

    private final CopyOnWriteArrayList<Channel> channels = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> ignored = new CopyOnWriteArrayList<>();
    private final BlockingQueue<WhatsUp> news = new LinkedBlockingQueue<>(); // offer(e), poll(), peek()

    public User(String session, String username) {
        this.session = session;
        this.username = username;
        this.lastSync = LocalDateTime.now();
    }

    public static User Dummy(String username) {
        return new User(null, username);
    }

    public String getSession() {
        return session;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getLastSync() {
        return lastSync;
    }

    public CopyOnWriteArrayList<Channel> getChannels() {
        return channels;
    }

    public CopyOnWriteArrayList<String> getIgnored() {
        return ignored;
    }

    public BlockingQueue<WhatsUp> getNews() {
        return news;
    }

    public void updateLastSync() {
        this.lastSync = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
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
                .add("channels", channels)
                .add("news", news)
                .toString();
    }
}
