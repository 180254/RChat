package pl.nn44.rchat.protocol;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WhatsUp implements Serializable {

    private static final long serialVersionUID = -2493165937560638279L;

    private final String isoTime;
    private final What what;
    private final String channel;
    private final String username;
    private final String[] params;

    public WhatsUp(What what,
                   String channel,
                   String username,
                   String... params) {

        // LocalDateTime and long are not supported by xml-rpc
        this.isoTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        this.what = what;
        this.channel = channel;
        this.username = username;
        this.params = params.clone();
    }

    protected WhatsUp() {
        this.isoTime = null;
        this.what = null;
        this.channel = null;
        this.username = null;
        this.params = null;
    }

    public LocalDateTime getTime() {
        return LocalDateTime.parse(this.isoTime, DateTimeFormatter.ISO_DATE_TIME);
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
        return Objects.equal(isoTime, whatsUp.isoTime) &&
                what == whatsUp.what &&
                Objects.equal(channel, whatsUp.channel) &&
                Objects.equal(username, whatsUp.username) &&
                Objects.equal(params, whatsUp.params);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isoTime, what, channel, username, params);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("isoTime", isoTime)
                .add("what", what)
                .add("channel", channel)
                .add("username", username)
                .add("params", params)
                .toString();
    }

    public enum What {
        MESSAGE, // MESSAGE $channel $username(who-msg) some-text
        PRIVY, // MESSAGE $null $username(who-msg-to) username-who-msg-by some-text

        TOPIC, // TOPIC $channel $username(who-changed) some-text

        JOIN, // JOIN $channel $username(who-join)
        PART, // PART $channel $username(who-part)
        KICK, // KICK $channel $username(who-kicked) username-kicked-by
        BAN, // BAN $channel $username(who-banned) username-banned-by ON/OFF

        ADMIN, // ADMIN $channel $username(who-admin) username-admin-by ON/OFF
        IGNORE, // IGNORE $channel $username(who-ignored) username-who-ignored-by ON/OFF
    }
}
