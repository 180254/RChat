package pl.nn44.rchat.protocol.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class User implements Serializable {

    private static final long serialVersionUID = -5068302409312104587L;

    private final String channel;
    private final String username;
    private final boolean authorized;
    private final boolean ignored;
    private final boolean admin;
    private final boolean banned;

    public User(String channel,
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

    protected User() {
        this.channel = null;
        this.username = null;
        this.authorized = false;
        this.ignored = false;
        this.admin = false;
        this.banned = false;
    }

    // ---------------------------------------------------------------------------------------------------------------

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

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return Objects.equal(channel, user.channel) &&
                Objects.equal(username, user.username);
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
