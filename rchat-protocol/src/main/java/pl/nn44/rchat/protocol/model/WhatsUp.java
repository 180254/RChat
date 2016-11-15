package pl.nn44.rchat.protocol.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WhatsUp implements Serializable {

    private static final long serialVersionUID = -2493165937560638279L;
    private static final transient DateTimeFormatter DTF = DateTimeFormatter.ISO_DATE_TIME;

    private final String isoTime;
    private final What what;
    private final String[] params;

    public WhatsUp(What what, String[] params) {

        // LocalDateTime and long are not supported by xml-rpc
        this.isoTime = LocalDateTime.now().format(DTF);
        this.what = what;
        this.params = params.clone();
    }

    public static WhatsUp create(What what, String... params) {
        return new WhatsUp(what, params);
    }

    protected WhatsUp() {
        this.isoTime = null;
        this.what = null;
        this.params = null;
    }

    // ---------------------------------------------------------------------------------------------------------------

    public LocalDateTime getTime() {
        return LocalDateTime.parse(this.isoTime, DTF);
    }

    public What getWhat() {
        return what;
    }

    public String[] getParams() {
        return params.clone();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WhatsUp whatsUp = (WhatsUp) obj;
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
