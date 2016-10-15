package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class ChannelUser implements Serializable {

    private static final long serialVersionUID = -5068302409312104587L;

    private final String username;
    private final boolean authorized;
    private final boolean ignored;
    private final boolean admin;

    public ChannelUser(String username,
                       boolean authorized,
                       boolean ignored,
                       boolean admin) {

        this.authorized = authorized;
        this.username = username;
        this.ignored = ignored;
        this.admin = admin;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelUser that = (ChannelUser) o;
        return authorized == that.authorized &&
                ignored == that.ignored &&
                admin == that.admin &&
                Objects.equal(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username, authorized, ignored, admin);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("username", username)
                .add("authorized", authorized)
                .add("ignored", ignored)
                .add("admin", admin)
                .toString();
    }
}
