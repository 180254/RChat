package pl.nn44.rchat.server.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import pl.nn44.rchat.protocol.WhatsUp;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

public class SeUser {

    private final String session;
    private final String username;
    private LocalDateTime lastSync;

    private final CopyOnWriteArrayList<SeChannel> channels = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SeUser> ignored = new CopyOnWriteArrayList<>();
    private final BlockingQueue<WhatsUp> news = new LinkedBlockingQueue<>(); // offer(e), poll(), peek()

    public SeUser(String session, String username) {
        this.session = session;
        this.username = username;
        this.lastSync = LocalDateTime.now();
    }

    public static SeUser Dummy(String username) {
        return new SeUser(null, username);
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

    public CopyOnWriteArrayList<SeChannel> getChannels() {
        return channels;
    }

    public CopyOnWriteArrayList<SeUser> getIgnored() {
        return ignored;
    }

    public BlockingQueue<WhatsUp> getNews() {
        return news;
    }

    public void updateLastSync() {
        this.lastSync = LocalDateTime.now();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeUser user = (SeUser) o;
        return Objects.equal(
                username.toLowerCase(),
                user.username.toLowerCase()
        );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username.toLowerCase());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("session", session)
                .add("username", username)
                .add("lastSync", lastSync)
                .add("channels", channels.stream().map(SeChannel::getName))
                .add("ignored", ignored.stream().map(SeUser::getUsername))
                // .add("news", news)
                .toString();
    }
}
