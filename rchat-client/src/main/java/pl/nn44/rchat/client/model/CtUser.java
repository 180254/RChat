package pl.nn44.rchat.client.model;

import pl.nn44.rchat.protocol.RcChUser;

import java.text.MessageFormat;

public class CtUser {

    private RcChUser user;

    public CtUser(RcChUser user) {
        this.user = user;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public RcChUser getUser() {
        return user;
    }

    public void setUser(RcChUser user) {
        this.user = user;
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        String modes = "(";
        modes += user.isAuthorized() ? "a" : "";
        modes += user.isAdmin() ? "o" : "";
        modes += user.isIgnored() ? "i" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1}",
                user.getUsername(),
                modes
        ).trim();
    }
}
