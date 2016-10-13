package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;

public class WhatsUp implements Serializable {

    private static final long serialVersionUID = -2493165937560638279L;

    private final What what;
    private final String channel;
    private final String username;
    private final String param;

    public WhatsUp(What what,
                   String channel,
                   String username,
                   String param) {

        this.what = what;
        this.channel = channel;
        this.username = username;
        this.param = param;
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

    public String getParam() {
        return param;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhatsUp whatsUp = (WhatsUp) o;
        return what == whatsUp.what &&
                Objects.equal(channel, whatsUp.channel) &&
                Objects.equal(username, whatsUp.username) &&
                Objects.equal(param, whatsUp.param);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(what, channel, username, param);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("what", what)
                .add("channel", channel)
                .add("username", username)
                .add("param", param)
                .toString();
    }

    enum What {
        MESSAGE, // MESSAGE $channel $username(who) some-text
        TOPIC, // TOPIC $channel $username(who) some-text
        NAME, // NAME $channel $username(who) JOIN/PART
        KICK // KICK $channel $username(who) $null
    }
}
