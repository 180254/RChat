package pl.nn44.rchat.client.impl;

import pl.nn44.rchat.protocol.RcChUser;

import java.text.MessageFormat;

public class CtUser {

    public final RcChUser rcChUser;

    public CtUser(RcChUser rcChUser) {
        this.rcChUser = rcChUser;
    }

    public String toString() {
        String modes = "(";
        modes += rcChUser.isAuthorized() ? "a" : "";
        modes += rcChUser.isAdmin() ? "o" : "";
        modes += rcChUser.isIgnored() ? "i" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1}",
                rcChUser.getUsername(),
                modes
        ).trim();
    }
}
