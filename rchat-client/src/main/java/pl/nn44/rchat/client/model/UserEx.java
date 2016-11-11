package pl.nn44.rchat.client.model;

import pl.nn44.rchat.protocol.RChUser;

import java.text.MessageFormat;

public class UserEx {

    public final RChUser rChUser;

    public UserEx(RChUser rChUser) {
        this.rChUser = rChUser;
    }

    public String toString() {
        String modes = "(";
        modes += rChUser.isAuthorized() ? "a" : "";
        modes += rChUser.isAdmin() ? "o" : "";
        modes += rChUser.isIgnored() ? "i" : "";
        modes += ")";

        modes = modes.length() > 2 ? modes : "";

        return MessageFormat.format(
                "{0} {1}",
                rChUser.getUsername(),
                modes
        ).trim();
    }
}
