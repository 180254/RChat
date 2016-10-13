package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class ChannelUser implements Serializable {

    private static final long serialVersionUID = -5068302409312104587L;

    private final String username;
    private final boolean ignored;
    private final boolean admin;

    public ChannelUser(String username,
                       boolean ignored,
                       boolean admin) {

        this.username = username;
        this.ignored = ignored;
        this.admin = admin;
    }

    public String getUsername() {
        return username;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public boolean isAdmin() {
        return admin;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelUser that = (ChannelUser) o;
        return ignored == that.ignored &&
                admin == that.admin &&
                Objects.equal(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username, ignored, admin);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("username", username)
                .add("ignored", ignored)
                .add("admin", admin)
                .toString();
    }
}
