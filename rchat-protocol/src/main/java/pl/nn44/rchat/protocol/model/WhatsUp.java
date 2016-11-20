package pl.nn44.rchat.protocol.model;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WhatsUp implements Serializable {

    private static final long serialVersionUID = -2493165937560638279L;
    private static final transient DateTimeFormatter DTF = DateTimeFormatter.ISO_DATE_TIME;

    private final String isoTime;
    private final What what;
    private final String[] params;

    // ---------------------------------------------------------------------------------------------------------------

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
        return LocalDateTime.parse(isoTime, DTF);
    }

    public What getWhat() {
        return what;
    }

    public String[] getParams() {
        return params.clone();
    }

    // ---------------------------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("isoTime", isoTime)
                .add("what", what)
                .add("params", params)
                .toString();
    }

    public enum What {
        // NOTHING is send on LOGIN & LOGOUT actions.
        // It helps speed up start(?)/stop(!) retrieving news.
        NOTHING,

        JOIN, // JOIN channel who-join is-auth is-admin
        PART, // PART channel who-part

        TOPIC, // TOPIC channel who-changed some-text

        KICK, // KICK channel who-kicked who-kicked-by
        BAN, // BAN channel who-banned who-banned-by ON/OFF
        ADMIN, // ADMIN channel who-admin username-admin-by ON/OFF
        IGNORE, // IGNORE unused who-ignored username-who-ignored-by ON/OFF

        MESSAGE, // MESSAGE channel who-msg some-text
        PRIVY, // PRIVY unused who-msg-to who-msg-by some-text
    }
}
