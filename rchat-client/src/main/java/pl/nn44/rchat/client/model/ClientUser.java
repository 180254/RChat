package pl.nn44.rchat.client.model;

import pl.nn44.rchat.protocol.model.User;

import java.text.MessageFormat;

public class ClientUser {

    private final String username;
    private boolean authorized;
    private boolean ignored;
    private boolean admin;
    private boolean banned;

    // ---------------------------------------------------------------------------------------------------------------

    public ClientUser(User user) {
        this.username = user.getUsername();
        this.authorized = user.isAuthorized();
        this.ignored = user.isIgnored();
        this.admin = user.isAdmin();
        this.banned = user.isBanned();
    }

    // ---------------------------------------------------------------------------------------------------------------

    public String getUsername() {
        return username;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public void setIgnored(boolean ignored) {
        this.ignored = ignored;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        String modes = "(";
        modes += authorized ? "a" : "";
        modes += admin ? "o" : "";
        modes += ignored ? "i" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1}",
                username,
                modes
        ).trim();
    }
}
