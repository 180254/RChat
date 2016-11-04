package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class RChUser implements Serializable {

    private static final long serialVersionUID = -5068302409312104587L;

    private final String channel;
    private final String username;
    private final boolean authorized;
    private final boolean ignored;
    private final boolean admin;
    private final boolean banned;

    public RChUser(String channel,
                   String username,
                   boolean authorized,
                   boolean ignored,
                   boolean admin,
                   boolean banned) {

        this.channel = channel;
        this.username = username;
        this.authorized = authorized;
        this.ignored = ignored;
        this.admin = admin;
        this.banned = banned;
    }

    protected RChUser() {
        this.channel = null;
        this.username = null;
        this.authorized = false;
        this.ignored = false;
        this.admin = false;
        this.banned = false;
    }

    public String getChannel() {
        return channel;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isBanned() {
        return banned;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RChUser that = (RChUser) o;
        return Objects.equal(channel, that.channel) &&
                Objects.equal(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(channel, username);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("channel", channel)
                .add("username", username)
                .add("authorized", authorized)
                .add("ignored", ignored)
                .add("admin", admin)
                .add("banned", banned)
                .toString();
    }
}
