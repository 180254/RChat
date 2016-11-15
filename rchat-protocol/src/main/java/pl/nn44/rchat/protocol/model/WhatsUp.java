package pl.nn44.rchat.protocol.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WhatsUp implements Serializable {

    private static final long serialVersionUID = -2493165937560638279L;
    private static final transient DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;

    private final String isoTime;
    private final What what;
    private final String[] params;

    public WhatsUp(What what,
                   String... params) {

        // LocalDateTime and long are not supported by xml-rpc
        this.isoTime = LocalDateTime.now().format(dtf);
        this.what = what;
        this.params = params;
    }

    protected WhatsUp() {
        this.isoTime = null;
        this.what = null;
        this.params = null;
    }

    public LocalDateTime getTime() {
        return LocalDateTime.parse(this.isoTime, dtf);
    }

    public What getWhat() {
        return what;
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
                Objects.equal(params, whatsUp.params);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isoTime, what, params);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("isoTime", isoTime)
                .add("what", what)
                .add("params", params)
                .toString();
    }

    public enum What {
        MESSAGE, // MESSAGE channel who-msg some-text
        PRIVY, // MESSAGE null who-msg-to who-msg-by some-text

        JOIN, // JOIN channel who-join is-auth is-admin
        PART, // PART channel who-part
        KICK, // KICK channel who-kicked who-kicked-by
        BAN, // BAN channel who-banned who-banned-by ON/OFF

        ADMIN, // ADMIN channel who-admin username-admin-by ON/OFF
        IGNORE, // IGNORE null who-ignored username-who-ignored-by ON/OFF

        TOPIC, // TOPIC channel who-changed some-text
    }
}
