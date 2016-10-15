package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class WhatsUp implements Serializable {

    private static final long serialVersionUID = -2493165937560638279L;

    private final What what;
    private final String channel;
    private final String username;
    private final String[] params;

    public WhatsUp(What what,
                   String channel,
                   String username,
                   String... params) {

        this.what = what;
        this.channel = channel;
        this.username = username;
        this.params = params.clone();
    }

    public What getWhat() {
        return what;
    }

    public String getChannel() {
        return channel;
    }

    public String getUsername() {
        return username;
    }

    public String[] getParams() {
        return params.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhatsUp whatsUp = (WhatsUp) o;
        return what == whatsUp.what &&
                Objects.equal(channel, whatsUp.channel) &&
                Objects.equal(username, whatsUp.username) &&
                Objects.equal(params, whatsUp.params);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(what, channel, username, params);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("what", what)
                .add("channel", channel)
                .add("username", username)
                .add("params", params)
                .toString();
    }

    public enum What {
        MESSAGE, // MESSAGE $channel $username(who-msg) some-text
        TOPIC, // TOPIC $channel $username(who-changed) some-text

        JOIN, // JOIN $channel $username(who-join)
        PART, // PART $channel $username(who-part)
        KICK, // KICK $channel $username(who-kicked) username-kicked-by
        BAN // BAN $channel $username(who-banned) username-banned-by
    }
}
